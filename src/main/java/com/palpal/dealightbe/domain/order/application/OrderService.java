package com.palpal.dealightbe.domain.order.application;

import static com.palpal.dealightbe.domain.store.domain.StoreStatus.CLOSED;
import static com.palpal.dealightbe.global.error.ErrorCode.CLOSED_STORE;
import static com.palpal.dealightbe.global.error.ErrorCode.INVALID_ITEM_QUANTITY;
import static com.palpal.dealightbe.global.error.ErrorCode.NOT_FOUND_ITEM;
import static com.palpal.dealightbe.global.error.ErrorCode.NOT_FOUND_MEMBER;
import static com.palpal.dealightbe.global.error.ErrorCode.NOT_FOUND_ORDER;
import static com.palpal.dealightbe.global.error.ErrorCode.NOT_FOUND_STORE;
import static com.palpal.dealightbe.global.error.ErrorCode.UNAUTHORIZED_REQUEST;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palpal.dealightbe.domain.item.domain.Item;
import com.palpal.dealightbe.domain.item.domain.ItemRepository;
import com.palpal.dealightbe.domain.member.domain.Member;
import com.palpal.dealightbe.domain.member.domain.MemberRepository;
import com.palpal.dealightbe.domain.notification.application.NotificationService;
import com.palpal.dealightbe.domain.order.application.dto.request.OrderCreateReq;
import com.palpal.dealightbe.domain.order.application.dto.request.OrderProductReq;
import com.palpal.dealightbe.domain.order.application.dto.request.OrderStatusUpdateReq;
import com.palpal.dealightbe.domain.order.application.dto.response.OrderRes;
import com.palpal.dealightbe.domain.order.application.dto.response.OrderStatusUpdateRes;
import com.palpal.dealightbe.domain.order.application.dto.response.OrdersRes;
import com.palpal.dealightbe.domain.order.domain.Order;
import com.palpal.dealightbe.domain.order.domain.OrderItem;
import com.palpal.dealightbe.domain.order.domain.OrderItemRepository;
import com.palpal.dealightbe.domain.order.domain.OrderRepository;
import com.palpal.dealightbe.domain.order.domain.OrderStatus;
import com.palpal.dealightbe.domain.store.domain.Store;
import com.palpal.dealightbe.domain.store.domain.StoreRepository;
import com.palpal.dealightbe.global.error.exception.BusinessException;
import com.palpal.dealightbe.global.error.exception.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final MemberRepository memberRepository;
	private final StoreRepository storeRepository;
	private final ItemRepository itemRepository;
	private final OrderItemRepository orderItemRepository;
	private final NotificationService notificationService;

	public OrderRes create(OrderCreateReq orderCreateReq, Long memberProviderId) {
		long storeId = orderCreateReq.storeId();
		Member member = getMember(memberProviderId);
		Store store = getStore(storeId);

		Order order = OrderCreateReq.toOrder(orderCreateReq, member, store);

		List<OrderItem> orderItems = createOrderItems(order, orderCreateReq.orderProductsReq().orderProducts());

		order.addOrderItems(orderItems);
		orderRepository.save(order);

		notificationService.send(member, store, order, OrderStatus.RECEIVED);

		return OrderRes.from(order);
	}

	public OrderStatusUpdateRes updateStatus(Long orderId, OrderStatusUpdateReq request, Long memberProviderId) {
		Member member = getMember(memberProviderId);
		Order order = getOrder(orderId);
		Long storeId = order.getStore().getId();
		Store store = getStore(storeId);

		String changedStatus = request.status();
		order.changeStatus(member, changedStatus);
		notificationService.send(member, store, order, OrderStatus.valueOf(changedStatus));

		return OrderStatusUpdateRes.from(order);
	}

	@Transactional(readOnly = true)
	public OrderRes findById(Long orderId, Long memberProviderId) {
		Order order = getOrder(orderId);
		Member member = getMember(memberProviderId);

		order.validateOrderUpdater(member);

		return OrderRes.from(order);
	}

	@Transactional(readOnly = true)
	public OrdersRes findAllByStoreId(Long storeId, Long memberProviderId, String status, Pageable pageable) {
		Store store = getStore(storeId);

		if (!store.isSameOwnerAndTheRequester(memberProviderId)) {
			throw new BusinessException(UNAUTHORIZED_REQUEST);
		}

		Slice<Order> orders = orderRepository.findAllByStoreId(storeId, status, pageable);

		return OrdersRes.from(orders);
	}

	@Transactional(readOnly = true)
	public OrdersRes findAllByMemberProviderId(Long memberProviderId, String status, Pageable pageable) {
		Slice<Order> orders = orderRepository.findAllByMemberProviderId(memberProviderId, status, pageable);

		return OrdersRes.from(orders);
	}

	private Store getStore(Long storeId) {
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> {
				log.warn("GET:READ:NOT_FOUND_STORE_BY_ID : {}", storeId);
				return new EntityNotFoundException(NOT_FOUND_STORE);
			});

		if (store.getStoreStatus().equals(CLOSED)) {
			throw new BusinessException(CLOSED_STORE);
		}

		return store;
	}

	private Order getOrder(Long orderId) {
		return orderRepository.findById(orderId)
			.orElseThrow(() -> {
				log.warn("GET:READ:NOT_FOUND_ORDER_BY_ID : {}", orderId);

				return new EntityNotFoundException(NOT_FOUND_ORDER);
			});
	}

	private Member getMember(Long memberProviderId) {
		return memberRepository.findMemberByProviderId(memberProviderId)
			.orElseThrow(() -> {
				log.warn("GET:READ:NOT_FOUND_MEMBER_BY_PROVIDER_ID : {}", memberProviderId);

				return new EntityNotFoundException(NOT_FOUND_MEMBER);
			});
	}

	private List<OrderItem> createOrderItems(Order order, List<OrderProductReq> orderProductsReq) {
		return orderProductsReq.stream()
			.map(productReq -> createOrderItem(order, productReq))
			.toList();
	}

	private OrderItem createOrderItem(Order order, OrderProductReq request) {
		long itemId = request.itemId();
		int quantity = request.quantity();

		int orderedCount = itemRepository.updateStock(itemId, quantity);

		if (orderedCount == 0) {
			throw new BusinessException(INVALID_ITEM_QUANTITY);
		}

		Item item = itemRepository.findByIdIgnoringStatus(itemId)
			.orElseThrow(() -> {
				log.warn("GET:READ:NOT_FOUND_ITEM_BY_ID : {}", itemId);

				return new EntityNotFoundException(NOT_FOUND_ITEM);
			});

		return OrderProductReq.toOrderItem(item, order, request);

	}
}
