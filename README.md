spray-example
=============

This is a simple example of how to use the
[Fidesmo API](https://developer.fidesmo.com/api) with
[spray](http://spray.io/). It implements three different services, one
using the transceive API (service ID: `transceive`), one using the
MIFARE Classic API (service ID: `mifare`) and one using the CCM API
(service ID: `install`):
- The transceive service delivery phase consists of 4 steps
  - sending SELECT
  - awaiting the result
  - sending SELECT again
  - awaiting the result
  - sending service completed
- The mifare service deliver phase consists of 4 or 6 steps
  - get a mifare card
  - awaiting the result
  - if the card is new call initialization of keys
  - awaiting the result
  - write 1k of mifare data
  - awaiting the result
  - sending service completed
- The install service deliver phase consists of 2 steps
  - install app
  - awaiting the result
  - sending service completed

The service delivery logic is implemented by a separate
"DeliveryActor" for each service. There is also an alternative version
of the mifare service, `mifare-pay`, which works just as the mifare
service but has a price.

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
`transceive` - this example SP has four services, `transceive`,
`install`, `mifare` and `mifare-pay`.
