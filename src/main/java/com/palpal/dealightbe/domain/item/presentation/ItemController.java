package com.palpal.dealightbe.domain.item.presentation;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.palpal.dealightbe.domain.item.application.ItemService;
import com.palpal.dealightbe.domain.item.application.dto.request.ItemReq;
import com.palpal.dealightbe.domain.item.application.dto.response.ItemRes;

@RequiredArgsConstructor
@RequestMapping("/api/items")
@RestController
public class ItemController {

	private final ItemService itemService;

	@PostMapping
	public ResponseEntity<ItemRes> create(@Validated @RequestBody ItemReq itemReq, @RequestParam Long memberId) {
		ItemRes itemRes = itemService.create(itemReq, memberId);

		return ResponseEntity.ok(itemRes);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<ItemRes> update(@PathVariable("id") Long itemId, @Validated @RequestBody ItemReq itemReq, @RequestParam Long memberId) {
		ItemRes itemRes = itemService.update(itemId, itemReq, memberId);

		return ResponseEntity.ok(itemRes);
	}
}