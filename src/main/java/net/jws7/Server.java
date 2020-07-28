package net.jws7;

import static spark.Spark.post;
import static spark.Spark.before;
import static spark.Spark.options;
import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.net.ApiResource;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.stripe.model.Charge;
import com.stripe.model.ChargeCollection;

import java.io.BufferedWriter;
import java.io.FileWriter;

public class Server {
	
	private static Gson gson = new Gson();
	
    public static void main(String[] args) {
    	    	
    	// Setup
    	System.out.println("Starting server.java...");
    	Stripe.apiKey = "sk_test_UE6J4wpD9We4ZLKXwAUwwvlC";

    	// Handle request from client to create a PaymentIntent
    	post("/create-payment-intent", (request, response) -> {	
       	    System.out.println("Recieved a create-payment-intent request POST");
       	     
	       	CustomerCreateParams customerParams = new CustomerCreateParams.Builder().build();
	        Customer customer = Customer.create(customerParams);
    	  	 		
    		// Create Payment Intent object
        	PaymentIntentCreateParams params =  PaymentIntentCreateParams.builder()
        	.setCurrency("gbp")
        	.setAmount(1099L)
        	.setCustomer(customer.getId())
        	.setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
        	// Verify your integration in this guide by including this parameter
        	.putMetadata("integration_check", "accept_a_payment")
        	.build();
    		
        	// Send payment intent to stripe and get client secret in return
        	PaymentIntent intent = PaymentIntent.create(params);
    		String paymentResponse = intent.getClientSecret();
    		
    		// Return client secret to client
    		String jsonResponse = gson.toJson(paymentResponse);
    		System.out.println("Sending secret back to client:"+jsonResponse);
    		return jsonResponse;
    	});
    	
    	post("/hooks", (request, response) -> {	
			System.out.println("Recieved a hooks handling request POST");
			
			// Parse response
			String payload = request.body();
			Event event = null;
    		
			try {
				event = ApiResource.GSON.fromJson(payload, Event.class);
			} catch (Exception e) {
				// Invalid payload
				response.status(400);
				return "";
			}
			
			// Deserialize the nested object inside the event
			EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
			StripeObject stripeObject = null;
			if (dataObjectDeserializer.getObject().isPresent()) {
				stripeObject = dataObjectDeserializer.getObject().get();
			} else {
				// Deserialization failed, probably due to an API version mismatch.
				System.out.println("Deserialization failed, with API version:"+ event.getApiVersion());
				response.status(400);
				return "";
			}
			
			// Handle the event
			switch (event.getType()) {
				case "payment_intent.succeeded":
					System.out.println("case:payment_intent.succeeded");
					PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
					handlePaymentIntentSucceeded(paymentIntent);
				break;
				case "charge.succeeded":
					System.out.println("case:charge.succeeded");
					//Charge charge = (Charge) stripeObject;
					//handleChargeSucceeded(charge);
					break;
				case "payment_method.attached":
					System.out.println("case:payment_method.attached");
					//PaymentMethod paymentMethod = (PaymentMethod) stripeObject;
					//handlePaymentMethodAttached(paymentMethod);
					break;
				default:
					// Unexpected event type
					System.out.println("Unexpected event type:"+event.getType());
					response.status(400);
					return "";
			}
			response.status(200);
			return "";
    	});
  	    	
    	// Set HTTP Headers to allow javascript client to talk to server
    	options("/*", (request, response) -> {

			String accessControlRequestHeaders = request
					.headers("Access-Control-Request-Headers");
			if (accessControlRequestHeaders != null) {
				response.header("Access-Control-Allow-Headers",
						accessControlRequestHeaders);
			}

			String accessControlRequestMethod = request
					.headers("Access-Control-Request-Method");
			if (accessControlRequestMethod != null) {
				response.header("Access-Control-Allow-Methods",
						accessControlRequestMethod);
			}

			return "OK";
		});
    	
    	before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));
    }

	// Handle a successful payment
	private static void handlePaymentIntentSucceeded(PaymentIntent paymentIntent) {
		try {
			BufferedWriter output = new BufferedWriter(new FileWriter("orders.txt", true));
			 
			// Get charges from successful payment
			ChargeCollection chargeCollection = paymentIntent.getCharges();
			for(Charge charge : chargeCollection.autoPagingIterable()) {
				// Build order details
				String name = charge.getShipping().getName();
				String email = charge.getReceiptEmail();
				String address = charge.getShipping().getAddress().getLine1();
				
				// Write out file
				String toWrite = "Order received from:"+name+", email:"+email+", address:"+address;
				System.out.println("Order:"+toWrite);
				output.newLine();
				output.append(toWrite);
			}
			// Close file			
			output.close();
		} catch (Exception e) {
			System.out.println("Exception");
			e.printStackTrace();
		}
	}
}