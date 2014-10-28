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
import me.binglu.ebookshop.ShoppingCart;

/**
 *
 * @author Bing Lu
 */
public class QueryServlet extends HttpServlet {

    private DataSource pool;    //DB connection pool
    
    @Override
    public void init(ServletConfig config) throws ServletException {
      try {
         // Create a JNDI Initial context to be able to lookup the DataSource
         InitialContext ctx = new InitialContext();
         // Lookup the DataSource, which will be backed by a pool
         //   that the application server provides.
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
            out.println("<title>Servlet QueryServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet QueryServlet at " + request.getContextPath() + "</h1>");
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
 
      try {
         // Retrieve and process request parameters: "author" and "search"
         String author = request.getParameter("author");
         boolean hasAuthorParam = author != null && !author.equals("Select...");
         String searchWord = request.getParameter("search");
         boolean hasSearchParam = searchWord != null && ((searchWord = searchWord.trim()).length() > 0);
 
         out.println("<html><head><title>Query Results</title></head><body>");
         out.println("<h2>E-BookShop - Query Results</h2>");
 
         if (!hasAuthorParam && !hasSearchParam) {  // No params present
            out.println("<h3>Please select an author or enter a search term!</h3>");
            out.println("<p><a href='start'>Back to Select Menu</a></p>");
         } else {
            conn = pool.getConnection();    //get a connection from the pool
            stmt = conn.createStatement();
 
            // Form a SQL command based on the param(s) present
            StringBuilder sqlStr = new StringBuilder();  // more efficient than String
            sqlStr.append("SELECT * FROM books WHERE quantity > 0 AND (");
            if (hasAuthorParam) {
               sqlStr.append("author = '").append(author).append("'");
            }
            if (hasSearchParam) {
               if (hasAuthorParam) {
                  sqlStr.append(" OR ");
               }
               sqlStr.append("author LIKE '%").append(searchWord)
                     .append("%' OR title LIKE '%").append(searchWord).append("%'");
            }
            sqlStr.append(") ORDER BY author, title");
            //System.out.println(sqlStr);  // for debugging
            ResultSet rset = stmt.executeQuery(sqlStr.toString());
 
            if (!rset.next()) {  // Check for empty ResultSet (no book found)
               out.println("<h3>No book found. Please try again!</h3>");
               out.println("<p><a href='start'>Back to Select Menu</a></p>");
            } else {
               // Print the result in an HTML form inside a table
               out.println("<form method='get' action='./cart'>");
               out.println("<input type='hidden' name='todo' value='add' />");
               out.println("<table border='1' cellpadding='6'>");
               out.println("<tr>");
               out.println("<th>&nbsp;</th>");
               out.println("<th>AUTHOR</th>");
               out.println("<th>TITLE</th>");
               out.println("<th>PRICE</th>");
               out.println("<th>QUANTITY</th>");
               out.println("<th>IN STOCK</th>");
               out.println("</tr>");
 
               // ResultSet's cursor now pointing at first row
               do {
                  // Print each row with a checkbox identified by book's id
                  String id = rset.getString("id");
                  out.println("<tr>");
                  out.println("<td><input type='checkbox' name='id' value='" + id + "' /></td>");
                  out.println("<td>" + rset.getString("author") + "</td>");
                  out.println("<td>" + rset.getString("title") + "</td>");
                  out.println("<td>$" + rset.getString("price") + "</td>");
                  out.println("<td><input type='text' size='3' value='1' name='quantity" + id + "' /></td>");
                  out.println("<td>" + rset.getString("quantity") + "</td>");
                  out.println("</tr>");
               } while (rset.next());
               out.println("</table><br />");      
               
               // Submit and reset buttons
               out.println("<input type='submit' value='Add to My Shopping Cart' />");
               out.println("<input type='reset' value='CLEAR' /></form>");
 
               // Hyperlink to go back to search menu
               out.println("<p><a href='start'>Back to Select Menu</a></p>");
               
               // Show "View Shopping Cart" if cart is not empty
               HttpSession session = request.getSession(false); // check if session exists
               if (session != null) {
                  ShoppingCart cart;
                  synchronized (session) {
                     // Retrieve the shopping cart for this session, if any. Otherwise, create one.
                     cart = (ShoppingCart) session.getAttribute("cart");
                     if (cart != null && !cart.isEmpty()) {
                        out.println("<p><a href='cart?todo=view'>View Shopping Cart</a></p>");
                     }
                  }
               }
               
               out.println("</body></html>");
            }
         }
         
      } catch (SQLException ex) {
         out.println("<h3>Service not available. Please try again later!</h3></body></html>");
         Logger.getLogger(QueryServlet.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
         out.close();
         try {
            if (stmt != null) stmt.close();
            if (conn != null) conn.close(); //return connection to pool
         } catch (SQLException ex) {
            Logger.getLogger(QueryServlet.class.getName()).log(Level.SEVERE, null, ex);
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
