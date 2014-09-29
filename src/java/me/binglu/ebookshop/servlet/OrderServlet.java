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
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import me.binglu.ebookshop.InputFilter;

/**
 *
 * @author Bing Lu
 */
public class OrderServlet extends HttpServlet {

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
            out.println("<title>Servlet OrderServlet</title>");            
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet OrderServlet at " + request.getContextPath() + "</h1>");
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

        try {
            out.println("<html><head><title>Order Confirmation</title></head><body>");
            out.println("<h2>E-BookShop - Order Confirmation</h2>");

            // Retrieve and process request parameters: id(s), cust_name, cust_email, cust_phone
            String[] ids = request.getParameterValues("id");  // Possibly more than one values
            String customerName = request.getParameter("customer_name");
            boolean hasCustomerName = customerName != null && ((customerName = InputFilter.htmlFilter(customerName.trim())).length() > 0);
            String customerEmail = request.getParameter("customer_email").trim();
            boolean hasCustomerEmail = customerEmail != null && ((customerEmail = InputFilter.htmlFilter(customerEmail.trim())).length() > 0);
            String customerPhone = request.getParameter("customer_phone").trim();
            boolean hasCustomerPhone = customerPhone != null && ((customerPhone = InputFilter.htmlFilter(customerPhone.trim())).length() > 0);

            // Validate inputs
            if (ids == null || ids.length == 0) {
                out.println("<h3>Please Select a Book!</h3>");
            } else if (!hasCustomerName) {
                out.println("<h3>Please Enter Your Name!</h3>");
            } else if (!hasCustomerEmail || (customerEmail.indexOf('@') == -1)) {
                out.println("<h3>Please Enter Your e-mail (user@host)!</h3>");
            } else if (!hasCustomerPhone || !InputFilter.isValidPhone(customerPhone)) {
                out.println("<h3>Please Enter an 10-digit Phone Number!</h3>");
            } else {
                // We shall build our output in a buffer, so that it will not be interrupted
                //  by error messages.
                StringBuilder outBuf = new StringBuilder();
                // Display the name, email and phone (arranged in a table)
                outBuf.append("<table>");
                outBuf.append("<tr><td>Customer Name:</td><td>").append(customerName).append("</td></tr>");
                outBuf.append("<tr><td>Customer Email:</td><td>").append(customerEmail).append("</td></tr>");
                outBuf.append("<tr><td>Customer Phone Number:</td><td>").append(customerPhone).append("</td></tr></table>");

                Class.forName("com.mysql.jdbc.Driver");
                conn = pool.getConnection();    //get connection from the pool
                stmt = conn.createStatement();
                // We shall manage our transaction (because multiple SQL statements issued)
                conn.setAutoCommit(false);

                // Print the book(s) ordered in a table
                outBuf.append("<br />");
                outBuf.append("<table border='1' cellpadding='6'>");
                outBuf.append("<tr><th>AUTHOR</th><th>TITLE</th><th>PRICE</th><th>QUANTITY</th></tr>");

                boolean error = false;
                float totalPrice = 0f;
                for (String id : ids) {
                    sqlStr = "SELECT * FROM books WHERE id = " + id;
                    //System.out.println(sqlStr);  // for debugging
                    rset = stmt.executeQuery(sqlStr);

                    // Expect only one row in ResultSet
                    rset.next();
                    int quantityAvailable = rset.getInt("quantity");
                    String title = rset.getString("title");
                    String author = rset.getString("author");
                    float price = rset.getFloat("price");

                    // Validate quantity ordered
                    String quantityOrderedStr = request.getParameter("quantity" + id);
                    int quantityOrdered = InputFilter.parsePositiveInt(quantityOrderedStr);
                    if (quantityOrdered == 0) {
                        out.println("<h3>Please Enter a valid quantity for \"" + title + "\"!</h3>");
                        error = true;
                        break;
                    } else if (quantityOrdered > quantityAvailable) {
                        out.println("<h3>There are insufficient copies of \"" + title + "\" available!</h3>");
                        error = true;
                        break;
                    } else {
                        // Okay, update the books table and insert an order record
                        sqlStr = "UPDATE books SET quantity = quantity - " + quantityOrdered + " WHERE id = " + id;
                        //System.out.println(sqlStr);  // for debugging
                        stmt.executeUpdate(sqlStr);

                        sqlStr = "INSERT INTO order_records values ("
                                + id + ", " + quantityOrdered + ", '" + customerName + "', '"
                                + customerEmail + "', '" + customerPhone + "')";
                        //System.out.println(sqlStr);  // for debugging
                        stmt.executeUpdate(sqlStr);

                        // Display this book ordered
                        outBuf.append("<tr>");
                        outBuf.append("<td>").append(author).append("</td>");
                        outBuf.append("<td>").append(title).append("</td>");
                        outBuf.append("<td>").append(price).append("</td>");
                        outBuf.append("<td>").append(quantityOrdered).append("</td></tr>");
                        totalPrice += price * quantityOrdered;
                    }
                }

                if (error) {
                    conn.rollback();
                } else {
                    // No error, print the output from the StringBuilder.
                    out.println(outBuf.toString());
                    out.println("<tr><td colspan='4' align='right'>Total Price: $");
                    out.printf("%.2f</td></tr>", totalPrice);
                    out.println("</table>");

                    out.println("<h3>Thank you.</h3>");
                    out.println("<p><a href='start'>Back to Select Menu</a></p>");
                    // Commit for ALL the books ordered.
                    conn.commit();
                }
            }
            out.println("</body></html>");
        } catch (ClassNotFoundException e) {
            System.out.println("Where is your jdbc driver?");
            e.printStackTrace();
        } catch (SQLException ex) {
            try {
                conn.rollback();  // rollback the updates
                out.println("<h3>Service not available. Please try again later!</h3></body></html>");
            } catch (SQLException ex1) {
            }
            Logger.getLogger(OrderServlet.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            out.close();
            try {
                if (stmt != null) {
                    stmt.close();
                }
                if (conn != null) {
                    conn.close();   //return connection to pool
                }
            } catch (SQLException ex) {
                Logger.getLogger(OrderServlet.class.getName()).log(Level.SEVERE, null, ex);
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
