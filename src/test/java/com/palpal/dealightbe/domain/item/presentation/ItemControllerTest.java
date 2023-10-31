package com.palpal.dealightbe.domain.item.presentation;

import java.time.LocalTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.palpal.dealightbe.domain.item.application.ItemService;
import com.palpal.dealightbe.domain.item.application.dto.request.ItemReq;
import com.palpal.dealightbe.domain.item.application.dto.response.ItemRes;
import com.palpal.dealightbe.domain.item.domain.Item;
import com.palpal.dealightbe.domain.store.domain.DayOff;
import com.palpal.dealightbe.domain.store.domain.Store;
import com.palpal.dealightbe.global.error.ErrorCode;
import com.palpal.dealightbe.global.error.exception.BusinessException;

import static com.palpal.dealightbe.global.error.ErrorCode.ALREADY_REGISTERED_ITEM_NAME;
import static com.palpal.dealightbe.global.error.ErrorCode.STORE_HAS_NOT_ITEM;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.STRING;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ItemController.class, excludeAutoConfiguration = {SecurityAutoConfiguration.class,
	OAuth2ClientAutoConfiguration.class})
@AutoConfigureRestDocs
class ItemControllerTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@MockBean
	ItemService itemService;

	private Store store;
	private Item item;

	@BeforeEach
	void setUp() {
		LocalTime openTime = LocalTime.now();
		LocalTime closeTime = openTime.plusHours(1);

		if (closeTime.isBefore(openTime)) {
			LocalTime tempTime = openTime;
			openTime = closeTime;
			closeTime = tempTime;
		}

		store = Store.builder()
			.name("동네분식")
			.storeNumber("0000000")
			.telephone("00000000")
			.openTime(openTime)
			.closeTime(closeTime)
			.dayOff(Collections.singleton(DayOff.MON))
			.build();

		item = Item.builder()
			.name("떡볶이")
			.stock(2)
			.discountPrice(4000)
			.originalPrice(4500)
			.description("기본 떡볶이 입니다.")
			.information("통신사 할인 불가능 합니다.")
			.store(store)
			.build();
	}

	@DisplayName("상품 등록 성공 테스트")
	@Test
	public void itemCreateSuccessTest() throws Exception {
		//given
		ItemReq itemReq = new ItemReq(item.getName(), item.getStock(), item.getDiscountPrice(), item.getOriginalPrice(), item.getDescription(), item.getInformation(), item.getImage());
		ItemRes itemRes = ItemRes.from(item);

		Long memberId = 1L;

		when(itemService.create(itemReq, memberId)).thenReturn(itemRes);

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.post("/api/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value(itemRes.name()))
			.andExpect(jsonPath("$.stock").value(itemRes.stock()))
			.andExpect(jsonPath("$.discountPrice").value(itemRes.discountPrice()))
			.andExpect(jsonPath("$.originalPrice").value(itemRes.originalPrice()))
			.andExpect(jsonPath("$.description").value(itemRes.description()))
			.andExpect(jsonPath("$.information").value(itemRes.information()))
			.andExpect(jsonPath("$.image").value(itemRes.image()))
			.andDo(print())
			.andDo(document("item-create",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(parameterWithName("memberId").description("고객 ID")
				),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("itemId").description("상품 ID"),
					fieldWithPath("storeId").description("업체 ID"),
					fieldWithPath("name").description("상품 이름"),
					subsectionWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				)
			));
	}

	@DisplayName("상품 등록 실패 테스트 - 입력되지 않은 상품 이름")
	@Test
	public void itemCreateFailureTest_invalidName() throws Exception {
		//given
		Item item2 = Item.builder()
			.name("")
			.stock(2)
			.discountPrice(4000)
			.originalPrice(4500)
			.description("기본 떡볶이 입니다.")
			.information("통신사 할인 불가능 합니다.")
			.store(store)
			.build();

		ItemReq itemReq = new ItemReq(item2.getName(), item2.getStock(), item2.getDiscountPrice(), item2.getOriginalPrice(), item2.getDescription(), item2.getInformation(), item2.getImage());
		ItemRes itemRes = ItemRes.from(item2);

		Long memberId = 1L;

		when(itemService.create(itemReq, memberId)).thenReturn(itemRes);

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.post("/api/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("C001"))
			.andExpect(jsonPath("$.errors[0].field").value("name"))
			.andExpect(jsonPath("$.errors[0].value").value(""))
			.andExpect(jsonPath("$.errors[0].reason").isNotEmpty())
			.andExpect(jsonPath("$.errors[1].field").value("name"))
			.andExpect(jsonPath("$.errors[1].value").value(""))
			.andExpect(jsonPath("$.errors[1].reason").isNotEmpty())
			.andExpect(jsonPath("$.message").value("잘못된 값을 입력하셨습니다."))
			.andDo(print())
			.andDo(document("item-create-fail-invalid-name",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("errors[].field").type(STRING).description("오류 필드"),
					fieldWithPath("errors[].value").type(STRING).description("오류 값"),
					fieldWithPath("errors[].reason").type(STRING).description("오류 사유"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}

	@DisplayName("상품 등록 실패 테스트 - 할인가가 원가보다 큰 경우")
	@Test
	public void itemCreateFailureTest_invalidDiscountPrice() throws Exception {
		//given
		ItemReq itemReq = new ItemReq("떡볶이", 2, 4500, 4000, "기본 떡볶이 입니다.", "통신사 할인 불가능 합니다.", null);

		Long memberId = 1L;

		when(itemService.create(any(), anyLong())).thenThrow(new BusinessException(ErrorCode.INVALID_ITEM_DISCOUNT_PRICE));

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.post("/api/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("I001"))
			.andExpect(jsonPath("$.errors").isEmpty())
			.andExpect(jsonPath("$.message").value("상품 할인가는 원가보다 클 수 없습니다."))
			.andDo(print())
			.andDo(document("item-create-fail-invalid-discount-price",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}

	@DisplayName("상품 등록 실패 테스트 - 이미 등록된 상품인 경우(이름 중복)")
	@Test
	public void itemCreateFailureTest_duplicatedItemName() throws Exception {
		//given
		ItemReq itemReq = new ItemReq("떡볶이", 2, 4500, 4000, "기본 떡볶이 입니다.", "통신사 할인 불가능 합니다.", null);

		Long memberId = 1L;

		doThrow(new BusinessException(ALREADY_REGISTERED_ITEM_NAME)).when(
			itemService).create(any(), any());

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.post("/api/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("I003"))
			.andExpect(jsonPath("$.errors").isEmpty())
			.andExpect(jsonPath("$.message").value("동일한 이름을 가진 상품이 이미 등록되어 있습니다."))
			.andDo(print())
			.andDo(document("item-create-fail-duplicated-item-name",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}

	@DisplayName("상품 수정 성공 테스트")
	@Test
	public void itemUpdateSuccessTest() throws Exception {
		//given
		Long itemId = 1L;
		Long memberId = 1L;

		ItemReq itemReq = new ItemReq("치즈 떡볶이", 4, 9900, 14000, "치즈 떡볶이 입니다.", "통신사 할인 가능 합니다.", null);
		ItemRes itemRes = new ItemRes(itemId, 1L, itemReq.name(), itemReq.stock(), itemReq.discountPrice(), itemReq.originalPrice(), itemReq.description(), itemReq.information(), itemReq.image());

		when(itemService.update(any(), any(), any())).thenReturn(itemRes);

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/items/{id}", itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value(itemRes.name()))
			.andExpect(jsonPath("$.stock").value(itemRes.stock()))
			.andExpect(jsonPath("$.discountPrice").value(itemRes.discountPrice()))
			.andExpect(jsonPath("$.originalPrice").value(itemRes.originalPrice()))
			.andExpect(jsonPath("$.description").value(itemRes.description()))
			.andExpect(jsonPath("$.information").value(itemRes.information()))
			.andExpect(jsonPath("$.image").value(itemRes.image()))
			.andDo(print())
			.andDo(document("item-update",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(parameterWithName("id").description("상품 ID")),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("itemId").description("상품 ID"),
					fieldWithPath("storeId").description("업체 ID"),
					fieldWithPath("name").description("상품 이름"),
					subsectionWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				)
			));
	}

	@DisplayName("상품 수정 실패 테스트 - 입력되지 않은 상품 이름")
	@Test
	public void itemUpdateFailureTest_invalidName() throws Exception {
		//given
		Long itemId = 1L;
		Long memberId = 1L;

		Item item2 = Item.builder()
			.name("")
			.stock(4)
			.discountPrice(9900)
			.originalPrice(14500)
			.description("치즈 떡볶이 입니다.")
			.information("통신사 할인 가능 합니다.")
			.store(store)
			.build();

		ItemReq itemReq = new ItemReq(item2.getName(), item2.getStock(), item2.getDiscountPrice(), item2.getOriginalPrice(), item2.getDescription(), item2.getInformation(), item2.getImage());
		ItemRes itemRes = ItemRes.from(item2);

		when(itemService.update(any(), any(), any())).thenReturn(itemRes);

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/items/{id}", itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("C001"))
			.andExpect(jsonPath("$.errors[0].field").value("name"))
			.andExpect(jsonPath("$.errors[0].value").value(""))
			.andExpect(jsonPath("$.errors[0].reason").isNotEmpty())
			.andExpect(jsonPath("$.errors[1].field").value("name"))
			.andExpect(jsonPath("$.errors[1].value").value(""))
			.andExpect(jsonPath("$.errors[1].reason").isNotEmpty())
			.andExpect(jsonPath("$.message").value("잘못된 값을 입력하셨습니다."))
			.andDo(print())
			.andDo(document("item-update-fail-invalid-name",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(parameterWithName("id").description("상품 ID")),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("errors[].field").type(STRING).description("오류 필드"),
					fieldWithPath("errors[].value").type(STRING).description("오류 값"),
					fieldWithPath("errors[].reason").type(STRING).description("오류 사유"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}

	@DisplayName("상품 수정 실패 테스트 - 할인가가 원가보다 큰 경우")
	@Test
	public void itemUpdateFailureTest_invalidDiscountPrice() throws Exception {
		//given
		ItemReq itemReq = new ItemReq("치즈 떡볶이", 4, 10000, 5000, "치즈 떡볶이 입니다.", "통신사 할인 가능 합니다.", null);

		Long memberId = 1L;
		Long itemId = 1L;

		when(itemService.update(any(), any(), any())).thenThrow(new BusinessException(ErrorCode.INVALID_ITEM_DISCOUNT_PRICE));

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/items/{id}", itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("I001"))
			.andExpect(jsonPath("$.errors").isEmpty())
			.andExpect(jsonPath("$.message").value("상품 할인가는 원가보다 클 수 없습니다."))
			.andDo(print())
			.andDo(document("item-update-fail-invalid-discount-price",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(parameterWithName("id").description("상품 ID")),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}

	@DisplayName("상품 수정 실패 테스트 - 이미 등록된 상품인 경우(이름 중복)")
	@Test
	public void itemUpdateFailureTest_duplicatedItemName() throws Exception {
		//given
		ItemReq itemReq = new ItemReq("떡볶이", 2, 4500, 4000, "기본 떡볶이 입니다.", "통신사 할인 불가능 합니다.", null);

		Long memberId = 1L;
		Long itemId = 1L;

		doThrow(new BusinessException(ALREADY_REGISTERED_ITEM_NAME)).when(
			itemService).update(any(), any(), any());

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/items/{id}", itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(itemReq))
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("I003"))
			.andExpect(jsonPath("$.errors").isEmpty())
			.andExpect(jsonPath("$.message").value("동일한 이름을 가진 상품이 이미 등록되어 있습니다."))
			.andDo(print())
			.andDo(document("item-update-fail-duplicated-item-name",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("stock").description("재고 수"),
					fieldWithPath("discountPrice").description("할인가"),
					fieldWithPath("originalPrice").description("원가"),
					fieldWithPath("description").description("상세 설명"),
					fieldWithPath("information").description("안내 사항"),
					fieldWithPath("image").description("상품 이미지")
				),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}

	@DisplayName("상품 삭제 성공 테스트")
	@Test
	public void itemDeleteSuccessTest() throws Exception {
		//given
		Long itemId = 1L;
		Long memberId = 1L;

		doNothing().when(itemService).delete(any(), any());
		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/items/{id}", itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.param("memberId", memberId.toString()))
			.andExpect(status().isOk())
			.andDo(print())
			.andDo(document("item-delete",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(parameterWithName("id").description("상품 ID")),
				requestParameters(parameterWithName("memberId").description("고객 ID"))
			));
	}

	@DisplayName("상품 삭제 실패 테스트 - 요청된 상품이 해당 업체에 등록되지 않은 상품인 경우")
	@Test
	public void itemDeleteFailureTest_storeHasNotItem() throws Exception {
		//given
		Long itemId = 1L;
		Long memberId = 1L;

		doThrow(new BusinessException(STORE_HAS_NOT_ITEM)).when(
			itemService).delete(any(), any());

		//when
		//then
		mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/items/{id}", itemId)
				.contentType(MediaType.APPLICATION_JSON)
				.param("memberId", memberId.toString()))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.timestamp").isNotEmpty())
			.andExpect(jsonPath("$.code").value("I005"))
			.andExpect(jsonPath("$.errors").isEmpty())
			.andExpect(jsonPath("$.message").value("요청하신 상품은 해당 업체에 등록되지 않은 상품입니다."))
			.andDo(print())
			.andDo(document("item-delete-store-has-not-item",
				Preprocessors.preprocessRequest(prettyPrint()),
				preprocessResponse(prettyPrint()),
				pathParameters(parameterWithName("id").description("상품 ID")),
				requestParameters(parameterWithName("memberId").description("고객 ID")),
				responseFields(
					fieldWithPath("timestamp").type(STRING).description("예외 시간"),
					fieldWithPath("code").type(STRING).description("예외 코드"),
					fieldWithPath("errors[]").type(ARRAY).description("오류 목록"),
					fieldWithPath("message").type(STRING).description("오류 메시지")
				)
			));
	}
}
