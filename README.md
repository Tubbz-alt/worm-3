<h1 align="center">Worm</h1>

<div align="center">
  Bot that Finds daily popular tweets.
</div>

[![Build Status](https://travis-ci.org/akadir/worm.svg?branch=master)](https://travis-ci.org/akadir/worm)
[![gradle-version](https://img.shields.io/badge/gradle-5.5.1-brightgreen)](https://img.shields.io/badge/gradle-5.5.1-brightgreen)

### TODO 

update readme.md

### Usage
clone project and set required properties in worm.properties and worm.auth files, and then execute: 

```groovy
gradle run
```

#### worm.properties

```properties
statusLimitToKeep=30
minFollowingCount=50
maxFollowingCount=2000
maxFollowersCount=200000
languageKey=tr
quoteLimit=13
quoteHour=22
quoteMinute=00
```

#### auth.properties
```properties
#Twitter API Auth keys and options
finder-consumer-key=
finder-consumer-secret=
finder-access-token=
finder-access-token-secret=
quoter-consumer-key=
quoter-consumer-secret=
quoter-access-token=
quoter-access-token-secret=
```

<div align="center">
  Developed with ❤︎ by <a href="https://github.com/akadir">akadir</a>
</div>