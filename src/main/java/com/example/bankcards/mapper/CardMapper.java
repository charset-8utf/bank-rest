package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CardMapper {

    @Mapping(target = "maskedCardNumber", source = "maskedNumber")
    @Mapping(target = "ownerUsername", source = "card.owner.username")
    CardResponse toResponse(Card card, String maskedNumber);
}
