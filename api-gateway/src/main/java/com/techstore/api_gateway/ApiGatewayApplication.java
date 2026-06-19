package com.techstore.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("user-service", r -> r.path("/api/v1/users/**", "/api/v1/admin/users/**")
						.filters(f -> f.rewritePath("/api/v1/users/(?<segment>v3/api-docs.*)", "/${segment}"))
						.uri("lb://USER-SERVICE"))

				.route("product-service", r -> r.path(
								"/api/v1/products/**",
								"/api/v1/categories/**",
								"/api/v1/brands/**",
								"/api/v1/admin/products/**",
								"/api/v1/admin/categories/**"
						)
						.filters(f -> f.rewritePath("/api/v1/(products|categories|brands|admin/products|admin/categories)/(?<segment>v3/api-docs.*)", "/${segment}"))
						.uri("lb://PRODUCT-SERVICE"))

				.route("cart-service", r -> r.path("/api/v1/carts/**")
						// Filter: Cắt bỏ /api/v1/carts
						.filters(f -> f.rewritePath("/api/v1/carts/(?<segment>v3/api-docs.*)", "/${segment}"))
						.uri("lb://CART-SERVICE"))

				.route("order-service", r -> r.path("/api/v1/orders/**", "/api/v1/admin/orders/**")
						// Filter: Cắt bỏ /api/v1/orders
						.filters(f -> f.rewritePath("/api/v1/(orders|admin/orders)/(?<segment>v3/api-docs.*)", "/${segment}"))
						.uri("lb://ORDER-SERVICE"))
				.build();
	}

	@Configuration
	public class CorsConfig {

		@Bean
		public CorsWebFilter corsWebFilter() {
			CorsConfiguration config = new CorsConfiguration();
			config.addAllowedOrigin("http://localhost:5173"); // Vite dev server
			config.addAllowedMethod("*");
			config.addAllowedHeader("*");
			config.setAllowCredentials(true);

			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", config);

			return new CorsWebFilter(source);
		}
	}
}