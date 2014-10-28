/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.binglu.ebookshop.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.*;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import me.binglu.ebookshop.CartItem;
import me.binglu.ebookshop.ShoppingCart;

/**
 *
 * @author Bing Lu
 */
public class CartServlet extends HttpServlet {
    
    private DataSource pool;    //Database connection pool
    
    @Override
    public void init(ServletConfig config) throws ServletException {
      try {
         // Create a JNDI Initial context to be able to lookup the DataSource
         InitialContext ctx = new InitialContext();
         // Lookup the DataSource.
         pool = (DataSource)ctx.lookup("java:comp/env/jdbc/mysql_ebookshop");
         if (pool == null)
            throw new ServletException("Unknown DataSource 'jdbc/mysql_ebookshop'");
      } catch (NamingException ex) {
         Logger.getLogger(EntryServlet.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    
    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet CartServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet CartServlet at " + request.getContextPath() + "</h1>");
            out.println("</body>");
            out.println("</html>");
        } finally {            
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter out = response.getWriter();
 
      // Retrieve current HTTPSession object. If none, create one.
      HttpSession session = request.getSession(true);
      ShoppingCart cart;
      synchronized (session) {  // synchronized to prevent concurrent updates
         // Retrieve the shopping cart for this session, if any. Otherwise, create one.
         cart = (ShoppingCart) session.getAttribute("cart");
         if (cart == null) {  // No cart, create one.
            cart = new ShoppingCart();
            session.setAttribute("cart", cart);  // Save it into session
         }
      }
 
      Connection conn   = null;
      Statement  stmt   = null;
      ResultSet  rset   = null;
      String    sqlStr = null;
 
      try {
         conn = pool.getConnection();  // Get a connection from the pool
         stmt = conn.createStatement();
 
         out.println("<html><head><title>Shopping Cart</title></head><body>");
         out.println("<h2>E-Bookshop - Your Shopping Cart</h2>");
 
         // This servlet handles 4 cases:
         // (1) todo=add id=1001 qty1001=5 [id=1002 qty1002=1 ...]
         // (2) todo=update id=1001 qty1001=5
         // (3) todo=remove id=1001
         // (4) todo=view
 
         String todo = request.getParameter("todo");
         if (todo == null) todo = "view";  // to prevent null pointer
 
         if (todo.equals("add") || todo.equals("update")) {
            // (1) todo=add id=1001 qty1001=5 [id=1002 qty1002=1 ...]
            // (2) todo=update id=1001 qty1001=5
            String[] ids = request.getParameterValues("id");
            if (ids == null) {
               out.println("<h3>Please Select a Book!</h3></body></html>");
               return;
            }
            for (String id : ids) {
               sqlStr = "SELECT * FROM books WHERE id = " + id;
               //System.out.println(sqlStr);  // for debugging
               rset = stmt.executeQuery(sqlStr);
               rset.next(); // Expect only one row in ResultSet
               String title = rset.getString("title");
               String author = rset.getString("author");
               float price = rset.getFloat("price");
               int instock = rset.getInt("quantity");
               
               // Get quantity ordered
               int quantityOrdered = Integer.parseInt(request.getParameter("quantity" + id));
               if(quantityOrdered > instock){
                   out.println("<h3>We have " + instock + " copies in stock. You can't order more than that!</h3>");
                   out.println("Please go back and try again.</body></html>");
                   return;
               }
               
               int idInt = Integer.parseInt(id);
               if (todo.equals("add")) {
                  cart.add(idInt, title, author, price, quantityOrdered);
               } else if (todo.equals("update")) {
                  cart.update(idInt, quantityOrdered);
               }
            }
 
         } else if (todo.equals("remove")) {
            String id = request.getParameter("id");  // Only one id for remove case
            cart.remove(Integer.parseInt(id));
         }
 
         // All cases - Always display the shopping cart
         if (cart.isEmpty()) {
            out.println("<p>Your shopping cart is empty</p>");
         } else {
            out.println("<table border='1' cellpadding='6'>");
            out.println("<tr>");
            out.println("<th>AUTHOR</th>");
            out.println("<th>TITLE</th>");
            out.println("<th>PRICE</th>");
            out.println("<th>QUANTITY</th>");
            out.println("<th>REMOVE</th></tr>");
 
            float totalPrice = 0f;
            for (CartItem item : cart.getAllItems()) {
               int id = item.getId();
               String author = item.getAuthor();
               String title = item.getTitle();
               float price = item.getPrice();
               int quantityOrdered = item.getQuantityOrdered();
 
               out.println("<tr>");
               out.println("<td>" + author + "</td>");
               out.println("<td>" + title +  "</td>");
               out.println("<td>" + price +  "</td>");
 
               //update item's quantity in shopping cart
               out.println("<td><form method='get' action='cart'>");
               out.println("<input type='hidden' name='todo' value='update' />");
               out.println("<input type='hidden' name='id' value='" + id + "' />");
               out.println("<input type='text' size='3' name='quantity"
                       + id + "' value='" + quantityOrdered + "' />" );
               out.println("<input type='submit' value='Update' />");
               out.println("</form></td>");
 
               //remove item from shopping cart
               out.println("<td><form method='get' action='cart'>");               
               out.println("<input type='hidden' name='todo' value='remove'/>");
               out.println("<input type='hidden' name='id' value='" + id + "'>");
               out.println("<input type='submit' value='Remove'>");
               out.println("</form></td>");
               out.println("</tr>");
               totalPrice += price * quantityOrdered;
            }
            out.println("<tr><td colspan='5' align='right'>Total Price: $");
            out.printf("%.2f</td></tr>", totalPrice);
            out.println("</table>");
         }
 
         out.println("<p><a href='start'>Select More Books...</a></p>");
 
         // Display the Checkout
         if (!cart.isEmpty()) {
            out.println("<br /><br />");
            out.println("<form method='get' action='checkout'>");
            out.println("<input type='submit' value='CHECK OUT'>");
            out.println("<p>Please fill in your particular before checking out:</p>");
            out.println("<table>");
            out.println("<tr>");
            out.println("<td>Enter your Name:</td>");
            out.println("<td><input type='text' name='customer_name' /></td></tr>");
            out.println("<tr>");
            out.println("<td>Enter your Email:</td>");
            out.println("<td><input type='text' name='customer_email' /></td></tr>");
            out.println("<tr>");
            out.println("<td>Enter your Phone Number:</td>");
            out.println("<td><input type='text' name='customer_phone' /></td></tr>");
            out.println("</table>");
            out.println("</form>");
         }
 
         out.println("</body></html>");
 
      } catch (SQLException ex) {
         out.println("<h3>Service not available. Please try again later!</h3></body></html>");
         Logger.getLogger(CartServlet.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
         out.close();
         try {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();  // return the connection to the pool
         } catch (SQLException ex) {
            Logger.getLogger(CartServlet.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
