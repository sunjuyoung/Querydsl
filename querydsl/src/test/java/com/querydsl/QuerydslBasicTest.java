package com.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.dto.MemberDto;
import com.querydsl.dto.QMemberDto;
import com.querydsl.dto.UserDto;
import com.querydsl.entity.Member;
import com.querydsl.entity.QMember;
import com.querydsl.entity.QTeam;
import com.querydsl.entity.Team;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

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


    @PersistenceUnit
    EntityManagerFactory emf;


    //검색조건
    @Test
    public void dynamicBooleanBuilder() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam,ageParam);


    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameParam != null){
            builder.and(member.username.eq(usernameParam));
        }
        if(ageParam !=null){
            builder.and(member.age.eq(ageParam));
        }

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
        return fetch;
    }

    @Test
    public void queryProjection() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(new QMemberDto(member.age, member.username)) //생성자방식과 다르게 컴파일 에러 가능
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }


        //service controller 여러 레이어에서 사용하는 dto가 querydsl 의존성을 갖게된다

    }


    @DisplayName("user dto조회 생성자 방식")
    @Test
    public void findUserDtoByConstruc() throws Exception {
        List<UserDto> fetch = queryFactory
                .select(Projections.constructor(UserDto.class,

                        member.username.as("name"),//필드 이름이 다를경우 별칭으로 매칭 가능
                        member.age


                ))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @DisplayName("user dto조회 ")
    @Test
    public void findUserDtoBy() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                        //member.age,
                        member.username.as("name"),//필드 이름이 다를경우 별칭으로 매칭 가능

                        ExpressionUtils.as(JPAExpressions
                        .select(memberSub.age.max())
                        .from(memberSub),"age")
                ))
                .from(member)
                .fetch();

         for (UserDto userDto : fetch) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @DisplayName("dto조회 생성자 방식")
    @Test
    public void findDtoByConstruct() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class, //생성자 파라미터 순서 일치해야한다.
                        member.age,
                        member.username))
                .from(member)
                .fetch();

        for(MemberDto memberDto:fetch){
            System.out.println("memberDto = " + memberDto);
        }
    }
    @DisplayName("dto조회 필드 방식")
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class, //필드에 직접 ,@setter(@Data) 필요없음
                        member.age,
                        member.username))
                .from(member)
                .fetch();

        for(MemberDto memberDto:fetch){
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("dto조회 bean setter 방식")
    @Test
    public void findDtoByQueryDSL() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,  //bean setter을 활용한 property 방식,
                        member.age,
                        member.username))
                .from(member)
                .fetch();

        for(MemberDto memberDto:fetch){
            System.out.println("memberDto = " + memberDto);
        }
    }


    //순수 JPA에서 DTO 조회할때 new 명령어를 사용해야한다
    @Test
    public void findDtoByJPQL() throws Exception {
        List<MemberDto> resultList = em.createQuery("select new com.querydsl.dto.MemberDto(m.age,m.username) from Member m", MemberDto.class)
                .getResultList();

        for(MemberDto memberDto:resultList){
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void tuple1() throws Exception {
        List<Tuple> fetch = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for(Tuple tuple : fetch){ //querldsl 종속적인 관계이기 때문에 repository내에서만 사용 권장 dto반환하여 앞단으로
            tuple.get(member.username);
            System.out.println("age = " + tuple.get(member.age));
        }
    }

    @Test
    public void concat() throws Exception {
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .fetch();
        for(String s: fetch){
            System.out.println("tuple = " +  s );
        }
    }

    @Test
    public void 상수더하기() throws Exception {
        List<Tuple> a = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for(Tuple s: a){
            System.out.println("tuple = " + s);
        }
    }

    @Test
    public void complexCase() throws Exception {
        List<String> ss
                = queryFactory.select(new CaseBuilder()
                .when(member.age.between(11, 12)).then("10살")
                .when(member.age.between(13, 14)).then("12살").otherwise("기타"))
                .from(member)
                .fetch();

        for(String s: ss){
            System.out.println("tuple = " + s);
        }
    }

    @Test
    public void casequery() throws Exception {
        List<String> fetch = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(11).then("열한살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for(String s: fetch){
            System.out.println("tuple = " + s);
        }
    }

    //from 절의 서브쿼리 한계
    @Test
    public void subQuerySelect() throws Exception {

        QMember memberSub = new QMember("memberSub");
        queryFactory
                .select(member.username,
                        JPAExpressions.select(memberSub.age.avg())
                        .from(memberSub)
                        )
                .from(member)
                .fetch();
    }

    //나이가 평균 보다 큰 회원
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
    }

    //나이가 가장 많은 회원을 조회
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
    }


    @Test
    public void fetchJoin() throws Exception {
        //given
        em.flush();
        em.clear();
        Member member2 = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member2.getTeam());
        assertThat(loaded).as("페치조인적용").isTrue();
    }

    @Test
    public void fetchJoi1() throws Exception {
        //given
        em.flush();
        em.clear();
        Member member1 = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(member1.getTeam());
        assertThat(loaded).as("페치조인미적용").isFalse();



    }

    //연관관계 없는 엔티티를 외부 조인 조회
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamB"));

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for(Tuple tuple: fetch){
            System.out.println("tuple = " + tuple);
        }


    }



    //연관관계 없는 엔티티를 조인
    //회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만, 회원은 모두 조회
    @Test
    public void join_on_filtering() throws Exception {
        //select m,t from member m left join m.team t on t.name = 'teamA'

        List<Tuple> teamA = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple: teamA){
            System.out.println("tuple = " + tuple);
        }


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
