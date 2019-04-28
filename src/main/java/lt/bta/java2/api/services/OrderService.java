package lt.bta.java2.api.services;

import lt.bta.java2.api.filters.AccessRoles;
import lt.bta.java2.api.filters.Role;
import lt.bta.java2.api.requests.AddCartLineRequest;
import lt.bta.java2.jpa.entities.*;
import lt.bta.java2.jpa.services.Dao;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * Cart operacijų servisas
 */
@Path("/order")
public class OrderService extends BaseService<Order> {

    // reikalaujama autorizacijos
    @Context
    private HttpServletRequest servletRequest;

    @Override
    protected Class<Order> getEntityClass() {
        return Order.class;
    }


    // add cart line in session
//    @AccessRoles({Role.USER})
    @POST
    @Path("/buy")
    public Response buy() {

        HttpSession session = servletRequest.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Dao<Cart> cartDao = new Dao<>(Cart.class);
        Cart userCart = cartDao.read(user.getId(), Cart.GRAPH_CART_LINES);
        if (userCart == null || userCart.getCartLines() == null || userCart.getCartLines().size() == 0) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Dao<Order> orderDao = createDao();
        Order order = new Order();
        order.setDate(LocalDate.now());
        order.setUser(user);
        order.setTotal(userCart.getTotal());
        order.setOrderLines(new HashSet<>());
        for (CartLine cartLine : userCart.getCartLines()) {

            OrderLine orderLine = new OrderLine();
            orderLine.setOrder(order);
            orderLine.setProduct(cartLine.getProduct());
            orderLine.setQty(cartLine.getQty());
            orderLine.setPrice(cartLine.getProduct().getPrice());
            order.getOrderLines().add(orderLine);
        }
        orderDao.create(order);

        userCart.getCartLines().clear();
        userCart.setTotal(BigDecimal.ZERO);
        userCart.setUser(user);
        cartDao.update(userCart);
        session.setAttribute("cart", userCart);

        return Response.ok(order).build();
    }

}
