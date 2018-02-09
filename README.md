# Battlecode 2018 Competition (post-mortem)

Our bot managed to get its way to the top 16, and make it over to MIT for the finalists tournament - where we eventually placed 9th. Here, I want to go into some detail about how our bot worked and analyze what we could've done better (or rather, what other teams did better than us). Hopefully future Battlecoders will find this useful!

First thing - let's talk about the game and specs.

## The Game - Escape to Mars

I'll be pretty brief here, so if you want a more thorough description you can view the specs here.

The win condition for Battlecode is fairly simple - get your bots to kill the other team's bots. However, I think Elon would be proud this year - the game's primary premise was an Escape to Mars. Basically, your bots all started on Earth, but by turn 750 (3/4 of the way through the game) Earth was completely flooded. This meant that if Earth flooded before you killed the enemy, you'd have to make it to and then fight for control of Mars in order to win.

Complicating this prisoner's-dilemma type situation are the different types of units:

* Workers - these are the only units that you start with. They can clone themselves and lay down Factories or Rockets.
* Factories - these are "buildings" that produce any type of unit (except Rockets).
* Rangers - the "shooting" unit - these units can deal damage at long range.
* Knights - fast, durable meelee units.
* Mages - "Glass cannons." Extremely low health, but can deal devastating splash damage if they get close to the enemy. Can also unlock an ability that lets them teleport.
* Healers - aptly named, they heal units.
* Rockets - these are the vehicles that can get you to Mars. They're capable of holding 8 (or more, if you upgrade them) units, and can take off to bring them over to Mars.

Maps would consist of an "Earth" and a "Mars", with certain areas that couldn't be accessed by units.

Notably, there was some new stuff that differed from previous years:
* Global information - previously, each bot only had access to its limited set of "senses", but this year information could be shared globally between bots.
* Time-based limits - previously, the amount of "computation" your bots had was limited a concept called "Bytecode", which was roughly related to some metric of the number of instructions. The switch to adding new languages (Python, Rust, C) necessitated a switch to computation time. Personally, I (and most people I talked to) weren't a fan of this change - as computation time differs based on platform and other factors and is much less predictable.


I think how we did can mostly be summed up in a series of mistakes. So I'm gonna talk about those mistakes, in order.

TODO

## A word about the competition

I don't want to be too critical, because I know the devs were mostly new this year. That being said, I think there were a few execution issues with Battlecode that made the competition somewhat problematic. This included massive bugs with the API (including a memory leak that caused nearly half of the teams to crash and instantly lose in the seeding tournament), a broken scrimmage ranking system (there was a bug with volatility that made it impossible for any teams to move up or down), an unfairly small international bracket, big game-altering balance changes mid-game, often poor maps, and an extremely annoying insistence by the devs on watching every scrimmage match at excruciatingly slow speed. (Seriously, when everyone watching is telling you to speed up, and literally no one is enjoying this except for you, you should probably listen... this honestly bothered me more than any of the other issues). Perhaps it's nostalgia, but I remember Battlecode 2017 being a smoother experience. I do hope the devs improve significantly next year; I don't know if I could deal with the frustration for one more year.

That being said, Battlecode isn't easy to run (I certainly wouldn't want to do it...) and I still think the devs did a great job. I really appreciate them putting in the work to get this up and running and would like to give them a word of thanks.

And as always,
Choo choo!
Neil
