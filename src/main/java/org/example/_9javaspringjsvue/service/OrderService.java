package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.OrderDTO;
import org.example._9javaspringjsvue.dto.OrderItemDTO;
import org.example._9javaspringjsvue.entity.*;
import org.example._9javaspringjsvue.repository.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    public OrderService(OrderRepository orderRepository,
                        CartService cartService,
                        CartRepository cartRepository,
                        CartItemRepository cartItemRepository,
                        ProductService productService,
                        UserRepository userRepository,
                        JavaMailSender mailSender) {
        this.orderRepository = orderRepository;
        this.cartService = cartService;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productService = productService;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    /**
     * ТЗ: "При нажатии на кнопку «Оформить заказ» происходит:
     * 1. Повторная проверка и уменьшение остатков
     * 2. Формирование и отправка письма на e-mail
     */
    @Transactional
    public OrderDTO createOrder(Long userId) {
        // 1. Получаем корзину пользователя
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Корзина пуста или не найдена"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new RuntimeException("Невозможно оформить пустую корзину");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new RuntimeException("У пользователя не указан email для отправки чека");
        }

        // 2. Создаем объект заказа
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.valueOf("NEW"));
        order.setCreatedAt(ZonedDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Обработка каждой позиции корзины
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            int quantity = cartItem.getQuantity();

            if (product.getStockQuantity() < quantity) {
                throw new RuntimeException(
                        "Не удалось оформить заказ. Товар \"" + product.getTitle() +
                                "\" недоступен в количестве " + quantity +
                                ". Остаток на складе: " + product.getStockQuantity()
                );
            }

            // Определение цены
            BigDecimal price = (product.getDiscountPrice() != null && product.getDiscountPrice().compareTo(BigDecimal.ZERO) > 0)
                    ? product.getDiscountPrice()
                    : product.getBasePrice();

            // Создание позиции заказа
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setPriceAtPurchase(price);

            orderItems.add(orderItem);

            // Подсчет общей суммы
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(quantity)));

            productService.decreaseStock(product.getId(), quantity);
        }

        order.setItems(orderItems);
        order.setTotalAmount(totalAmount);

        // Сохранение заказа в БД
        Order savedOrder = orderRepository.save(order);

        // 4. Очистка корзины после успешного оформления
        cartService.clearCart(userId);

        // 5. Реальная отправка через JavaMailSender
        /** try {
            sendOrderConfirmationEmail(user, savedOrder);
            System.out.println("Письмо успешно отправлено на: " + user.getEmail());
        } catch (MessagingException e) {
            // Если отправка не удалась, логируем ошибку, но заказ уже создан в БД
            System.err.println("Ошибка при отправке email подтверждения: " + e.getMessage());
            throw new RuntimeException("Заказ оформлен, но не удалось отправить письмо на почту.", e);
        }
*/
        System.out.println("Заказ № " + savedOrder.getId() + " успешно оформлен");

        return mapToDTO(savedOrder);
    }

    /**
     * Получение истории заказов пользователя
     */
    @Transactional(readOnly = true)
    public List<OrderDTO> getUserOrders(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        List<Order> orders = orderRepository.findByUser(user);
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Формирование и отправка HTML-письма
     */
    private void sendOrderConfirmationEmail(User user, Order order) throws javax.mail.MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        helper.setTo(user.getEmail());
        helper.setSubject("Подтверждение заказа №" + order.getId() + " в нашем магазине");
        helper.setFrom("noreply@shop.com");

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body style='font-family: Arial, sans-serif; color: #333;'>");
        htmlContent.append("<h2 style='color: #2c3e50;'>Спасибо за ваш заказ, ").append(user.getFirstName()).append("!</h2>");
        htmlContent.append("<p>Ваш заказ №<strong>").append(order.getId()).append("</strong> успешно оформлен.</p>");
        htmlContent.append("<p>Дата заказа: ").append(order.getCreatedAt()).append("</p>");

        htmlContent.append("<table style='width: 100%; border-collapse: collapse; margin-top: 20px; border: 1px solid #ddd;'>");
        htmlContent.append("<thead style='background-color: #f8f9fa;'>");
        htmlContent.append("<tr>");
        htmlContent.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: left;'>Товар</th>");
        htmlContent.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: center;'>Количество</th>");
        htmlContent.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>Цена</th>");
        htmlContent.append("<th style='border: 1px solid #ddd; padding: 12px; text-align: right;'>Сумма</th>");
        htmlContent.append("</tr>");
        htmlContent.append("</thead><tbody>");

        for (OrderItem item : order.getItems()) {
            BigDecimal itemSum = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
            htmlContent.append("<tr>");
            htmlContent.append("<td style='border: 1px solid #ddd; padding: 12px;'>").append(item.getProduct().getTitle()).append("</td>");
            htmlContent.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: center;'>").append(item.getQuantity()).append("</td>");
            htmlContent.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(item.getPriceAtPurchase()).append(" ₽</td>");
            htmlContent.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right;'>").append(itemSum).append(" ₽</td>");
            htmlContent.append("</tr>");
        }

        htmlContent.append("</tbody>");
        htmlContent.append("<tfoot>");
        htmlContent.append("<tr style='font-weight: bold; font-size: 1.2em; background-color: #e9ecef;'>");
        htmlContent.append("<td colspan='3' style='border: 1px solid #ddd; padding: 12px; text-align: right;'>Итого к оплате:</td>");
        htmlContent.append("<td style='border: 1px solid #ddd; padding: 12px; text-align: right; color: #27ae60;'>").append(order.getTotalAmount()).append(" ₽</td>");
        htmlContent.append("</tr>");
        htmlContent.append("</tfoot>");
        htmlContent.append("</table>");

        htmlContent.append("<p style='margin-top: 20px; color: #7f8c8d;'>Статус заказа: <strong>").append(order.getStatus()).append("</strong></p>");
        htmlContent.append("<p>С уважением, команда Интернет-Магазина.</p>");
        htmlContent.append("</body></html>");

        helper.setText(htmlContent.toString(), true); // true означает HTML-контент

        mailSender.send((MimeMessagePreparator) message);
    }

    /**
     * Маппинг Entity -> DTO
     */
    private OrderDTO mapToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(String.valueOf(order.getStatus()));
        dto.setCreatedAt(order.getCreatedAt());

        List<OrderItemDTO> itemsDTO = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setProductId(item.getProduct().getId());
                itemDTO.setProductTitle(item.getProduct().getTitle());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPriceAtPurchase());
                itemDTO.setSubtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
                itemsDTO.add(itemDTO);
            }
        }
        dto.setItems(itemsDTO);
        return dto;
    }
}