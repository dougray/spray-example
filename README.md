spray-example
=============

This is a simple example of how to use the
[Fidesmo API](https://developer.fidesmo.com/api) with
[spray](http://spray.io/). It implements the following services:
- `transceive` - Simple service that sends some APDUs to the card
- `mifare` - A more complex service that initializes, reads and writes
  to a virtual MIFARE card
- `mifare-pay` - Same as above but with payment
- `install` - Installs a Java Card app using the CCM api
- `fail` - A service that does nothing but fail
- `fail-pay` - Same as above but with payment

The service delivery logic is implemented by a separate
"DeliveryActor" for each type of service.

Test server
-----------

Service provider `e26b8f12` is running this service for testing
purposes. Beware that it's running on free [Heroku](http://heroku.com)
instance so it might be quite slow due to spinning down! To test this
running service from an Android application call the
[Fidesmo App](https://developer.fidesmo.com/android) using the
following code:

```
Intent intent = new Intent();
intent.setAction("com.fidesmo.sec.DELIVER_SERVICE");
intent.setData(Uri.parse("https://api.fidesmo.com/service/e26b8f12/transceive"));
startActivity(intent);
```
The URI encodes the service provider `e26b8f12` and the service ID
`transceive` - this example SP implements all the services listed
above for easy testing.
