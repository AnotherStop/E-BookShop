/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package me.binglu.ebookshop;

/**
 *
 * @author Bing Lu
 */
public class CartItem {
   private int id;  //book id
   private String title;    //book title
   private String author;   //author
   private float price;     //price
   private int quantityOrdered;  //quantity ordered
 
   // Constructor
   public CartItem(int id, String title, String author, float price, int quantityOrdered) {
      this.id = id;
      this.title = title;
      this.author = author;
      this.price = price;
      this.quantityOrdered = quantityOrdered;
   }
 
   public int getId() {
      return id;
   }
 
   public String getAuthor() {
      return author;
   }
 
   public String getTitle() {
      return title;
   }
 
   public float getPrice() {
      return price;
   }
 
   public int getQuantityOrdered() {
      return quantityOrdered;
   }
 
   public void setQuantityOrdered(int newQuantity) {
      this.quantityOrdered = newQuantity;
   }    
}
