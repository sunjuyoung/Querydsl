package com.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.dto.MemberSearchCondition;
import com.querydsl.dto.MemberTeamDto;
import com.querydsl.dto.QMemberTeamDto;
import com.querydsl.entity.Member;

import com.querydsl.entity.QMember;
import com.querydsl.entity.QTeam;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static com.querydsl.entity.QMember.*;
import static com.querydsl.entity.QTeam.*;
import static org.springframework.util.ObjectUtils.isEmpty;

@Repository
@RequiredArgsConstructor
public class MemberJpaRepository {

    // 싱글톤 동시성문제? 스프링에서 프록시 가짜를 주입해서 트랜잭션단위로 각각 바인딩 해줘서 동시성문제 발생하지 않는다
    private final EntityManager em;
    private final JPAQueryFactory queryFactory; //bean 등록
    
    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member member = em.find(Member.class, id);
        return Optional.ofNullable(member);
    }

    public List<Member> findAll(){
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username){
        return queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq(username))
                .fetch();
    }

    //동적 쿼리 builder
    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if(condition.getAgeGoe() != null){
            builder.and(member.age.goe(condition.getAgeGoe()));
        }
        if(condition.getAgeLoe() != null){
            builder.and(member.age.loe(condition.getAgeLoe()));
        }
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),member.username,member.age
                        ,team.id.as("teamId")
                        ,team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)
                .fetch();
    }

    //동적 쿼리 where
    public List<MemberTeamDto> search(MemberSearchCondition condition){
        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId"),
                        member.username,
                        member.age
                        ,team.id.as("teamId")
                        ,team.name.as("teamName")
                ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))
                .fetch();
    }





    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe !=null ? member.age.loe(ageLoe) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe !=null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.hasText(teamName)? team.name.eq(teamName) : null;
    }

    private BooleanExpression usernameEq(String username) {
        return isEmpty(username)? null : member.username.eq(username);
    }
}
