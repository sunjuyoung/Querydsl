package com.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.entity.Member;
import com.querydsl.entity.QMember;
import com.querydsl.entity.QTeam;
import com.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import java.util.List;

import static com.querydsl.entity.QMember.member;
import static com.querydsl.entity.QTeam.team;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
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


    }

    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> fetch = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(fetch)
                .extracting("username")
                .containsExactly("teamA","teamB");

    }

    @Test
    public void aggregation(){
        List<Tuple> fetch = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg())
                .from(member)
                .fetch();

    }

    //팀의 이름과 각팀의 평균 연령을 구하라
    @Test
    public void group() throws Exception {
        List<Tuple> fetch = queryFactory
                .select(QTeam.team.name, member.age.avg())
                .from(member)
                .join(member.team, QTeam.team)
                .groupBy(QTeam.team.name)
                .fetch();

            assertThat(fetch.size()).isEqualTo(2);
            assertThat(fetch.get(0).get(QTeam.team.name)).isEqualTo("teamA");


    }

    /**
     */
    @Test
    public void paging1() {
        List<Member> fetch = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();
        assertThat(fetch.size()).isEqualTo(2);
    }

    //페이징 전체 조회 수가 필요할때 count 쿼리 따로 실행
    //조건문이 추가될때 사용하지않고 count쿼리 따로 작성이 좋다
    @Test
    public void paging2() {
        QueryResults<Member> fetch = queryFactory.selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();
        
        assertThat(fetch.getTotal()).isEqualTo(4);
        assertThat(fetch.getLimit()).isEqualTo(2);
        assertThat(fetch.getOffset()).isEqualTo(0);
    }



    /**
     * 1.나이 내림차순
     * 2.이름 올림차순
     * 2에서 회원 이름이 없으면 마지막에 출력 nulls last
     */
    @Test
    public void sort() {

        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        System.out.println(fetch.size());
        for(Member m : fetch){
            System.out.println(m.getAge());
        }
        assertThat(fetch.get(0).getUsername()).isEqualTo("member5");

    }

    @Test
    public void result1() {
/*
            List<Member> memberList  = queryFactory.selectFrom(member).fetch();

        //결과가 둘 이상이면 notUniqueResultException
        Member member = queryFactory.selectFrom(QMember.member).fetchOne();

        Member member1 = queryFactory.selectFrom(QMember.member).fetchFirst();
*/

        QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        long l = queryFactory.selectFrom(member).fetchCount();
    }

    @Test
    public void search2Querydsl() {
        Member member1 = queryFactory.select(member)
                .from(member)
                .where(
                        member.username.eq("member1"), //and 경우 콤마로 나열 가능 null경우 무시한다
                        (member.age.eq(10))
                )
                .fetchOne();
    }



        @Test
    public void startJPQL(){
        //member1 찾아ㅏㄹ
        Member memberJPQL = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(memberJPQL.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl(){

        //QMember m1 = member;
       // QMember m = new QMember("m");

        Member member1 = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchQuerydsl(){
        Member member1 = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        Member member2 = queryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1").and(member.age.between(10,12)))
                .fetchOne();

        assertThat(member1.getUsername()).isEqualTo("member1");
        assertThat(member1.getAge()).isEqualTo(10);

                /*
         member.username.eq("member1") // username = 'member1'
        member.username.ne("member1") //username != 'member1'
        member.username.eq("member1").not() // username != 'member1'
        member.username.isNotNull() //이름이 is not null
        member.age.in(10, 20) // age in (10,20)
        member.age.notIn(10, 20) // age not in (10, 20)
        member.age.between(10,30) //between 10, 30
        member.age.goe(30) // age >= 30
        member.age.gt(30) // age > 30
        member.age.loe(30) // age <= 30
        member.age.lt(30) // age < 30
        member.username.like("member%") //like 검색
        member.username.contains("member") // like ‘%member%’ 검색
        member.username.startsWith("member") //like ‘member%’ 검색
        * */
    }



}
