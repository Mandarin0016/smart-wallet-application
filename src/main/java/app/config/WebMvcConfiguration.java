package app.config;

import app.security.SessionCheckInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private SessionCheckInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        //** - всичко след
        registry.addInterceptor(interceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/images/**");
    }
}
