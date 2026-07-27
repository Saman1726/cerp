package io.cerp.identity.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<ProfileBypassFilter> profileBypassFilterRegistration() {
        FilterRegistrationBean<ProfileBypassFilter> registrationBean = new FilterRegistrationBean<>();
        ProfileBypassFilter filter = new ProfileBypassFilter();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns("/users/profile");
        registrationBean.setOrder(-110); // Run before Spring Security (which is -100 by default)
        return registrationBean;
    }
}
