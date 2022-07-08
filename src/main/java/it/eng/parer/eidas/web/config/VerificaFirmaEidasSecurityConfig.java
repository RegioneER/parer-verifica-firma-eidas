package it.eng.parer.eidas.web.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class VerificaFirmaEidasSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().authorizeRequests() // autorizza
                .antMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/swagger-ui.html").permitAll() // risorse
                                                                                                               // statiche
                .antMatchers("/v1/**").permitAll() // versione 1
                .antMatchers("/v2/**").permitAll() // versione 2
                .antMatchers("/actuator/shutdown").hasRole("ADMIN") // solo admin per shutdown
                .antMatchers("/admin/**").hasRole("ADMIN") // admin
                .and().formLogin().defaultSuccessUrl("/admin") // accedi ad admin
                .and().logout().logoutSuccessUrl("/").deleteCookies("JSESSIONID"); // procedura di logout
        /*
         * h2 console https://springframework.guru/using-the-h2-database-console-in-spring-boot- with-spring-security/
         */
        http.headers().frameOptions().disable();
    }

}
