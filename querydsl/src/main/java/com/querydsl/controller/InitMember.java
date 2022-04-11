package com.querydsl.controller;

import com.querydsl.entity.Member;
import com.querydsl.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    @PostConstruct // 스프링 라이프사이클 설정으로  @transactional과 같이 사용 불가하기 때문에
                                //init 메서드로 따로 분리해서 사용
    public void init(){
        initMemberService.init();
    }

    @Component
    static class InitMemberService{
        @PersistenceContext
        private EntityManager em;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for(int i=0; i<100; i++){
                Team selectTeam = i % 2 ==0 ? teamA : teamB;
                em.persist(new Member("member"+i,i,selectTeam));
            }
        }
    }
}
