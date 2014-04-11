spray-example
=============

This is a simple example of how to use the
[Fidesmo API](https://developer.fidesmo.com/api) with
[spray](http://spray.io/). It implements the following trivial use
case:

- Any service id supported
- The service delivery phase consists of 4 steps
  - sending SELECT
  - awaiting the result
  - sending SELECT again
  - awaiting the result
- After this the service is declared successfully completed. If there
  is any failure down the road, the service is declared completed but
  with a failure.

The service delivery logic is implemented by the DeliverService actor.

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
intent.setData(Uri.parse("https://api.fidesmo.com/service/e26b8f12/12345678"));
startActivity(intent);
```
The URI encodes the service provider `e26b8f12` and the service ID
`12345678` - this example SP only has one service, so it doesn't care
about the service id.
