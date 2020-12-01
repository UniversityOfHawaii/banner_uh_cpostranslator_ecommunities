package edu.hawaii.banner.cpos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	@Value("${rest.user}")
  String username;
  @Value("${rest.password}")
  String password;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
      http
        .authorizeRequests()

// Request CPOS translations
            .antMatchers("/student/**").access("hasRole(\"USER\") or hasRole(\"ADMIN\")")

// Actuator Checks - Allow for anyone with firewall access to this points
            .antMatchers("/actuator/health").permitAll()
            .antMatchers("/actuator/info").permitAll()

// Default everywhere else to only ADMINS
            .antMatchers("/**").access("hasRole(\"ADMIN\")")
        .anyRequest().authenticated()
        .and()
        .httpBasic();
      http.csrf().disable();
  
  }

	  @Bean
	  @Override
	  public UserDetailsService userDetailsService() {
	  	//System.out.println("username: " + username);
	  	//System.out.println("password: " + password);

	  	UserDetails user =
	           User.withDefaultPasswordEncoder()
	              .username(username)
	              .password(password)
	              .roles("ADMIN")
	              .build();

	      return new InMemoryUserDetailsManager(user);
	  }
}

