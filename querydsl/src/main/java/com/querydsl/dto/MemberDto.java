package com.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    private int age;
    private String username;

    @QueryProjection
    public MemberDto(int age, String username) {
        this.age = age;
        this.username = username;
    }
}
