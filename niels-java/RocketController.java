/**
 * General strategy:
 *
 - Past round 400, start building rockets
 - Rule: only build rockets if we have X number of factories being maintained, otherwise build factories
 - Once (round# < 500 && #rockets > 0):
 - Knowing rocket locations + capacity, pick X closest units (Maybe a little bigger than X in case some die) and tell them to run to rocket
 - All rockets take off at turn 749 lmao
 - Pick an axis, dump some rockets on one side, some on the other side
 */
public class RocketController {

}
