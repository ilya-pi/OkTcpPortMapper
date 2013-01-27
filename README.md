OkTcpPortMapper
===============

Pure Java Nio TCP Port Mapper

Tech Soup
---------

### Building

No extra moves are required, just go as you would with any other maven project.

To build —

    mvn clean install

### Running

To simple ways to run it, first — go with the speacilly designated for that maven profile, like this:

    mvn -Papp test

> NOTE:
> This would launch app with default configuration provided in the `proxy.properties`, can be found under
> `./src/main/resources/proxy.properties`; args are specified in `pom.xml`, executed in a separate
> to maven build thread

Second, build it and go manual:

    mvn clean install
    cd target/
    java -cp ./tcp-port-mapper-0.0-SNAPSHOT.jar com.ilyapimenov.applications.ok.TcpPortMapper proxy.properties

### Configuration

Configuration file should provide local port, remote host and remote port to map to.

Sample file goes as:

    ya.localPort = 8081
    ya.remoteHost = www.ya.ru
    ya.remotePort = 80

    ok.localPort = 8080
    ok.remoteHost = www.odnoklassniki.ru
    ok.remotePort = 80

    jabber.remoteHost = xmpp.odnoklassniki.ru
    jabber.remotePort = 5222
    jabber.localPort = 5222

    typo.rmoteHost = xmpp.odnoklassniki.ru
    typo.remotePort = 5222
    typo.localPort = 5222

    incomplete.remotePort = 6666
    incomplete.localPort = 5222

Here `localhost:8080` would be mapped to `www.odnoklassniki.ru:80`, while `localhost:5222` would be mapped to `xmpp.odnoklassniki.ru:5222`.

Records `localPort`, `remoteHost` and `remotePort` can go in any order, those met later override rules for the same name; unrecognized records are reported, missing records for a certain rule are warned to a customer.

### Kawabanga!

Feedback
--------

Please send me an [email](ilya.pimenov@gmail.com) with any feedback you have.

Links
-----

 * [Odnoklassniki](http://www.ok.ru/)
 * [Supplementary OK-working spirit information](http://v.ok.ru/)
