package com.techstore.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	// ĐÂY CHÍNH LÀ QUYỂN SỔ CHỈ ĐƯỜNG BẰNG CODE!
	// Trình biên dịch sẽ bảo vệ nó, không sợ sai lề như YAML nữa.
	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("identity-service", r -> r.path("/api/auth/**", "/api/users/**")
						.uri("lb://IDENTITY-SERVICE"))
				.build();
	}
}