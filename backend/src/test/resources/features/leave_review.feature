Feature: Leave Review
  As a renter
  I want to leave a review after a rental ends
  So that others know the item quality

  Scenario: Renter leaves review after rental ends
    Given an approved past booking exists for item "Switch" with renter "renter@example.com" and owner "owner@example.com"
    When the user "renter@example.com" leaves a review with rating 5 and comment "Great item" for the booking
    Then the item "Switch" should have a review with rating 5
