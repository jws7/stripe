// Set Stripe key
var stripe = Stripe('pk_test_SO6isqKbsrL4HMlvYvuGioY0');

console.log("Starting to fetch clientSecret");
fetch("http://localhost:4567/create-payment-intent", {
	method: "POST",
	headers: {
    "Content-Type": "application/json"
  	},
  	body: JSON.stringify("test")
})
.then(function(result){
	return result.json();
})
.then(function(data){
	console.log("fetch returned with:"+data);

	// Initalise Stripe Elements
	var elements = stripe.elements();

	// Create the card element and inject into the DOM
	var card = elements.create("card", {});
	card.mount("#card-element");

	// Handle / Show card errors
	card.on("change", function (event) {
	    document.querySelector("#card-error").textContent = event.error ? event.error.message : "";
	 });

	// Create listener for submit event
	var form = document.getElementById("payment-form");
	form.addEventListener("submit", function(event) {
  		event.preventDefault();
  		// Complete payment when the submit button is clicked
  		console.log("submit button clicked");
  		payWithCard(stripe, card, data);
	});
});

var payWithCard = function(stripe, card, clientSecret){
	console.log("starting payWithCard");

	var name = document.getElementById('name').value;
	var address = document.getElementById('address').value;
	var email = document.getElementById('email').value;
	console.log("confirmCardPayment for: name:"+name +", address:"+address+", email:"+email);

	stripe.confirmCardPayment(clientSecret, {
		payment_method: {
			card: card,
		},
		receipt_email: email,
		shipping: {
			name: name,
		   	address: {
		   		line1:address
		   	}
		},
	})
	.then(function(result){
		if(result.error){
			// Show error
			console.log("returned error:"+result.error.message);
			showError(result.error.message);
		} else {
			// Successful payment
			console.log("Succeeded!:"+result.paymentIntent.id);
			orderComplete(result.paymentIntent.id);
		}
	});
};

/* ------- UI helpers ------- */
// Shows a success message when the payment is complete
var orderComplete = function(paymentIntentId) {
  console.log("orderComplete:"+paymentIntentId);
  document.querySelector(".result-message").classList.remove("hidden");
  document.querySelector("button").disabled = true;
};
// Show the customer the error from Stripe if their card fails to charge
var showError = function(errorMsgText) {
  console.log("showError:"+errorMsgText);
  var errorMsg = document.querySelector("#card-error");
  errorMsg.textContent = errorMsgText;
};

// Create listener for buy button click
var buy = document.getElementById("buy-now");
buy.addEventListener("click", function(event) {
	event.preventDefault();
	console.log("buy-now button clicked");
	//Now unhide payments form
	document.getElementById("payment-form").classList.remove("hidden");
	document.getElementById("buy-now").classList.add("hidden");
	// todo: hide if already shown
});