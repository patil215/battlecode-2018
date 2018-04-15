# Battlecode 2018

This is the source code for our entry into the Battlecode 2018 competition.

Our team name was Steam Locomotive, composed of the following individuals:
* Neil Patil
* Niels Kornerup
* Ryan Rice
* John Herrick

Our team placed top 16 in the qualifying tournament, and was invited to MIT for the finalist's tournament - where we eventually placed 9th.

You can see a livestream of the competition [here](https://www.youtube.com/watch?v=QdljkowowC4).

A quick into to the game:

## The Game - Escape to Mars

I'll be pretty brief here, so if you want a more thorough description you can view the specs on the official Battlecode website.

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


## A bit about our strategy

TODO
