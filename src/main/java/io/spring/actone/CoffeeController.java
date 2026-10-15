package io.spring.actone;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CoffeeController {

	private static final List<MenuItem> MENU = List.of(
			new MenuItem("espresso", "Espresso", 3.00, "coffee"),
			new MenuItem("latte", "Latte", 4.50, "coffee"));
	private static final List<Order> ORDERS = List.of(
			new Order("order-1001", "Jordan Lee", List.of(new OrderItem("espresso", 2)), 6.00, "ready"));

	@GetMapping("/menu")
	public List<MenuItem> menu() {
		return MENU;
	}

	@GetMapping("/orders")
	public List<Order> orders() {
		return ORDERS;
	}

	record MenuItem(String id, String name, double priceUsd, String category) {}
	record OrderItem(String menuId, int quantity) {}
	record Order(String id, String customer, List<OrderItem> items, double totalUsd, String status) {}
}
