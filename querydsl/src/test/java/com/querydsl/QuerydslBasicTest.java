package com.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
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


    @Test
    public void sqlFunction() throws Exception {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace',{0},{1},{2})"
                        , member.username, "member","M"))
                .from(member)
                .fetch();

        for(String r : result){
            System.out.println("s  = "+r);
        }

    }

    @Test
    public void bulkDelete() throws Exception {
        queryFactory
                .delete(member)
                .where(member.age.gt(13))
                .execute();
    }


    @Test
    public void bulkAdd() throws Exception {
        //?????? ?????? ?????? 1 ?????????
      queryFactory
              .update(member)
              .set(member.age,member.age.add(1))// ????????? multiply
              .execute();
    }

    @Test
    public void bulkUpdate() throws Exception {
        //bulkupdate??? ?????????????????? ???????????? db??? ?????? ???????????? -> ????????????????????? db????????? ??????
        //????????? ???????????????????????? ?????? ??????
         queryFactory
                .update(member)
                .set(member.username,"?????????")
                .where(member.age.lt(13))
                .execute();
    }

    @Test
    public void dynamicQuery_where() throws Exception {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);

    }

    private List<Member> searchMember2(String usernameParam, Integer ageParam) {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameParam),ageEq(ageParam))
                //.where(allEq(ageParam,usernameParam)) //????????????
                .fetch();
        return fetch;
    }

    private BooleanExpression ageEq(Integer ageParam) {
        return ageParam == null ? null : member.age.eq(ageParam);
    }

    private BooleanExpression usernameEq(String usernameParam) {
        if(usernameParam !=null){
            return member.username.eq(usernameParam);
        }else{
            return null;
        }
    }
    //?????? ??????
    private BooleanExpression allEq(Integer ageParam,String username) {
       return usernameEq(username).and(ageEq(ageParam));
    }

    //????????????
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
                .select(new QMemberDto(member.age, member.username)) //?????????????????? ????????? ????????? ?????? ??????
                .from(member)
                .fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto = " + memberDto);
        }


        //service controller ?????? ??????????????? ???????????? dto??? querydsl ???????????? ????????????

    }


    @DisplayName("user dto?????? ????????? ??????")
    @Test
    public void findUserDtoByConstruc() throws Exception {
        List<UserDto> fetch = queryFactory
                .select(Projections.constructor(UserDto.class,

                        member.username.as("name"),//?????? ????????? ???????????? ???????????? ?????? ??????
                        member.age


                ))
                .from(member)
                .fetch();

        for (UserDto userDto : fetch) {
            System.out.println("memberDto = " + userDto);
        }
    }

    @DisplayName("user dto?????? ")
    @Test
    public void findUserDtoBy() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> fetch = queryFactory
                .select(Projections.fields(UserDto.class,
                        //member.age,
                        member.username.as("name"),//?????? ????????? ???????????? ???????????? ?????? ??????

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

    @DisplayName("dto?????? ????????? ??????")
    @Test
    public void findDtoByConstruct() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.constructor(MemberDto.class, //????????? ???????????? ?????? ??????????????????.
                        member.age,
                        member.username))
                .from(member)
                .fetch();

        for(MemberDto memberDto:fetch){
            System.out.println("memberDto = " + memberDto);
        }
    }
    @DisplayName("dto?????? ?????? ??????")
    @Test
    public void findDtoByField() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.fields(MemberDto.class, //????????? ?????? ,@setter(@Data) ????????????
                        member.age,
                        member.username))
                .from(member)
                .fetch();

        for(MemberDto memberDto:fetch){
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("dto?????? bean setter ??????")
    @Test
    public void findDtoByQueryDSL() throws Exception {
        List<MemberDto> fetch = queryFactory
                .select(Projections.bean(MemberDto.class,  //bean setter??? ????????? property ??????,
                        member.age,
                        member.username))
                .from(member)
                .fetch();

        for(MemberDto memberDto:fetch){
            System.out.println("memberDto = " + memberDto);
        }
    }


    //?????? JPA?????? DTO ???????????? new ???????????? ??????????????????
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

        for(Tuple tuple : fetch){ //querldsl ???????????? ???????????? ????????? repository???????????? ?????? ?????? dto???????????? ????????????
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
    public void ???????????????() throws Exception {
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
                .when(member.age.between(11, 12)).then("10???")
                .when(member.age.between(13, 14)).then("12???").otherwise("??????"))
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
                        .when(10).then("??????")
                        .when(11).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();
        for(String s: fetch){
            System.out.println("tuple = " + s);
        }
    }

    //from ?????? ???????????? ??????
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

    //????????? ?????? ?????? ??? ??????
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

    //????????? ?????? ?????? ????????? ??????
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
        assertThat(loaded).as("??????????????????").isTrue();
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
        assertThat(loaded).as("?????????????????????").isFalse();



    }

    //???????????? ?????? ???????????? ?????? ?????? ??????
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



    //???????????? ?????? ???????????? ??????
    //????????? ?????? ???????????????, ??? ????????? teamA??? ??????, ????????? ?????? ??????
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

    //?????? ????????? ????????? ?????? ????????? ?????????
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

    //????????? ?????? ?????? ?????? ???????????? count ?????? ?????? ??????
    //???????????? ???????????? ?????????????????? count?????? ?????? ????????? ??????
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
     * 1.?????? ????????????
     * 2.?????? ????????????
     * 2?????? ?????? ????????? ????????? ???????????? ?????? nulls last
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

        //????????? ??? ???????????? notUniqueResultException
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
                        member.username.eq("member1"), //and ?????? ????????? ?????? ?????? null?????? ????????????
                        (member.age.eq(10))
                )
                .fetchOne();
    }



        @Test
    public void startJPQL(){
        //member1 ????????????
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
        member.username.isNotNull() //????????? is not null
        member.age.in(10, 20) // age in (10,20)
        member.age.notIn(10, 20) // age not in (10, 20)
        member.age.between(10,30) //between 10, 30
        member.age.goe(30) // age >= 30
        member.age.gt(30) // age > 30
        member.age.loe(30) // age <= 30
        member.age.lt(30) // age < 30
        member.username.like("member%") //like ??????
        member.username.contains("member") // like ???%member%??? ??????
        member.username.startsWith("member") //like ???member%??? ??????
        * */
    }



}
