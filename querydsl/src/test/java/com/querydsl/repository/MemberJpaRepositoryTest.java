package com.querydsl.repository;

import com.querydsl.dto.MemberSearchCondition;
import com.querydsl.dto.MemberTeamDto;
import com.querydsl.entity.Member;


import com.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    public void basicTest() throws Exception {
        Member member = new Member("member1", 10);
        memberRepository.save(member);
        Member member1 = memberRepository.findById(member.getId()).get();

        Assertions.assertThat(member).isEqualTo(member1);

        List<Member> result1 = memberRepository.findAll();
        List<Member> byUsername = memberRepository.findByUsername(member.getUsername());
    }

    @Test
    public void searchTest() throws Exception {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",11,teamB);

        Member member3 = new Member("member3",13,teamA);
        Member member4 = new Member("member4",14,teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        //조건이 모두 널일 경우 조건없이 모두 가져오기 때문에 주의
        condition.setAgeGoe(13);
        condition.setAgeLoe(14);
        condition.setTeamName("teamB");
        

       // List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        //List<MemberTeamDto> result = memberJpaRepository.search(condition);
        List<MemberTeamDto> result = memberRepository.search(condition);
        Assertions.assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchPageTest() throws Exception {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",11,teamB);

        Member member3 = new Member("member3",13,teamA);
        Member member4 = new Member("member4",14,teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        PageRequest pageRequest = PageRequest.of(0,3);

        Page<MemberTeamDto> result = memberRepository.searchPageSimple(condition,pageRequest);
        // Assertions.assertThat(result).extracting("username").containsExactly("member4");

    }

}