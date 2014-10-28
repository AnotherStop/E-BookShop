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
public class CheckoutServlet extends HttpServlet {

   private DataSource pool;  // Database connection pool
 
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
            out.println("<title>Servlet CheckoutServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet CheckoutServlet at " + request.getContextPath() + "</h1>");
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
 
      Connection conn = null;
      Statement stmt = null;
      ResultSet rset = null;
      String sqlStr = null;
      HttpSession session = null;
      ShoppingCart cart = null;
 
      try {
         conn = pool.getConnection();  // Get a connection from the pool
         stmt = conn.createStatement();
 
         out.println("<html><head><title>Checkout</title></head><body>");
         out.println("<h2>E-Bookshop - Checkout</h2>");
 
         // Retrieve the Cart
         session = request.getSession(false);
         if (session == null) {
            out.println("<h3>Your Shopping cart is empty!</h3></body></html>");
            return;
         }
         synchronized (session) {
            cart = (ShoppingCart) session.getAttribute("cart");
            if (cart == null) {
               out.println("<h3>Your Shopping cart is empty!</h3></body></html>");
               return;
            }
         }
 
         // Retrieve and process request parameters: id(s), cust_name, cust_email, cust_phone
         String customerName = request.getParameter("customer_name");
         boolean hasCustomerName = customerName != null && ((customerName = customerName.trim()).length() > 0);
         String customerEmail = request.getParameter("customer_email").trim();
         boolean hasCustomerEmail = customerEmail != null && ((customerEmail = customerEmail.trim()).length() > 0);
         String customerPhone = request.getParameter("customer_phone").trim();
         boolean hasCustomerPhone = customerPhone != null && ((customerPhone = customerPhone.trim()).length() > 0);
 
         // Validate inputs
         if (!hasCustomerName) {

            out.println("<h3>Please Enter Your Name!</h3></body></html>");
            return;
         } else if (!hasCustomerEmail || (customerEmail.indexOf('@') == -1)) {
            out.println("<h3>Please Enter Your email (user@host)!</h3></body></html>");
            return;
         } else if (!hasCustomerPhone || customerPhone.length() != 8) {
            out.println("<h3>Please Enter an 8-digit Phone Number!</h3></body></html>");
            return;
         }
 
         // Display the name, email and phone (arranged in a table)
         out.println("<table>");
         out.println("<tr>");
         out.println("<td>Customer Name:</td>");
         out.println("<td>" + customerName + "</td></tr>");
         out.println("<tr>");
         out.println("<td>Customer Email:</td>");
         out.println("<td>" + customerEmail + "</td></tr>");
         out.println("<tr>");
         out.println("<td>Customer Phone Number:</td>");
         out.println("<td>" + customerPhone + "</td></tr>");
         out.println("</table>");
 
         // Print the book(s) ordered in a table
         out.println("<br />");
         out.println("<table border='1' cellpadding='6'>");
         out.println("<tr>");
         out.println("<th>AUTHOR</th>");
         out.println("<th>TITLE</th>");
         out.println("<th>PRICE</th>");
         out.println("<th>QUANTITY</th></tr>");
 
         float totalPrice = 0f;
         for (CartItem item : cart.getAllItems()) {
            int id = item.getId();
            String author = item.getAuthor();
            String title = item.getTitle();
            int quantityOrdered = item.getQuantityOrdered();
            float price = item.getPrice();
 
            // No check for price and qtyAvailable change
            // Update the books table and insert an order record
            sqlStr = "UPDATE books SET quantity = quantity - " + quantityOrdered + " WHERE id = " + id;
            //System.out.println(sqlStr);  // for debugging
            stmt.executeUpdate(sqlStr);
 
            sqlStr = "INSERT INTO order_records values ("
                    + id + ", " + quantityOrdered + ", '" + customerName + "', '"
                    + customerEmail + "', '" + customerPhone + "')";
            //System.out.println(sqlStr);  // for debugging
            stmt.executeUpdate(sqlStr);
 
            // Show the book ordered
            out.println("<tr>");
            out.println("<td>" + author + "</td>");
            out.println("<td>" + title + "</td>");
            out.println("<td>" + price + "</td>");
            out.println("<td>" + quantityOrdered + "</td></tr>");
            totalPrice += price * quantityOrdered;
         }
         out.println("<tr><td colspan='4' align='right'>Total Price: $");
         out.printf("%.2f</td></tr>", totalPrice);
         out.println("</table>");
 
         out.println("<h3>Thank you.</h3>");
         out.println("<a href='start'>Back to Search Menu</a>");
         out.println("</body></html>");
 
         cart.clear();   // empty the cart
      } catch (SQLException ex) {
         cart.clear();   // empty the cart
         out.println("<h3>Service not available. Please try again later!</h3></body></html>");
         Logger.getLogger(CheckoutServlet.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
         out.close();
         try {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();  // Return the connection to the pool
         } catch (SQLException ex) {
            Logger.getLogger(CheckoutServlet.class.getName()).log(Level.SEVERE, null, ex);
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
