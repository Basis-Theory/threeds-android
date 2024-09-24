const express = require("express");
var cors = require("cors");
var bodyParser = require("body-parser");
const { BasisTheory } = require("@basis-theory/basis-theory-js");

require('dotenv').config()

var app = express();

const port = 3333;

app.use(bodyParser.json());
app.use(cors());

let bt;
(async () => {
  bt = await new BasisTheory().init(process.env.BT_API_KEY_PVT, {
    apiBaseUrl: "https://api.flock-dev.com",
  });
})();

app.post("/3ds/authenticate", async (req, res) => {
  try {
    var payload = req.body;

    const response = await bt.threeds.authenticateSession(payload.sessionId, {
      authenticationCategory: "payment",
      authenticationType: "payment-transaction",
      purchaseInfo: {
        amount: "80000",
        currency: "826",
        exponent: "2",
        date: "20240109141010",
      },
      requestorInfo: {
        id: "example-3ds-merchant",
        name: "Example 3DS Merchant",
        url: "https://www.example.com/example-merchant",
      },
      merchantInfo: {
        mid: "9876543210001",
        acquirerBin: "000000999",
        name: "Example 3DS Merchant",
        categoryCode: "7922",
        countryCode: "826",
      },
      cardholderInfo: {
        name: "John Doe",
        email: "john@me.com",
      },
    });


    res.status(200).send({
      ...response,
      // include these to increase sucess rate during challenge evaluation
      merchantName: "Example 3DS Merchant",
      purchaseAmount: "80000",
      currency: "826",
    });
  } catch (e) {
    res.status(e.status).send(e.data);
  }
});


app.post("/3ds/get-result", async (req, res) => {
  try {
    var payload = req.body;

    const response = await bt.threeds.getChallengeResult(payload.sessionId);

    res.status(200).send({
      ...response,
    });
  } catch (e) {
    res.status(e.status).send(e.data);
  }
});

app.listen(port, () => {
  console.log(`This thing is running like Forest Gump on port: ${port}`);
});
