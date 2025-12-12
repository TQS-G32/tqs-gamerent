Feature: Owner Reviews Renter
  As an owner
  I want to leave a review for the renter after a booking
  So that other owners can assess renter behavior

  Scenario: Owner leaves review for renter after rental
    Given an approved past booking exists for item "PS4" with renter "renter@example.com" and owner "owner@example.com"
    When the user "owner@example.com" leaves a user review with rating 4 and comment "Good renter" for the booking
    Then the user "renter@example.com" should have a review with rating 4
