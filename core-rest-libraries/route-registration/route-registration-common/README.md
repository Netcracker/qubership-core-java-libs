Route-registration-common
------------------

This is an abstract library which presents common annotations and logic for any 
java frameworks and libraries, such as Spring, Quarkus. 
If you want to write your route-registration library which will be suitable to your framework 
then you should take this module and write a processor on the following annotations:
* Route: The annotation specify type of gateway. It can be internal, private, public.
* Gateway: Overrides the path by which the route will be registered in specified gateway
* FacadeRoute: Marks should the route be registered in Facade gateway 
* FacadeGateway: Overrides the path by which the route will be registered in the facade gateway
