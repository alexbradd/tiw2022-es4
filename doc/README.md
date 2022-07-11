# Documentation quick guide

A brief guide detailing each resource.

## Some terminology

Quick clarification on the naming used throughout the project:

1. "Account" is to be interpreted as "bank account" (conto bancario), not the user's account, unless specified
2. "User" indicates both the actor and the user's account

## IFML

1. `ifml-ria.png`: IFML diagram for the RIA version
2. `ifml-static.png`: IFML diagram for the HTML-only version

## Sequence diagrams

### Sequences for the HTML-only version (`static-seq/`)

|             File              |                   Event/Action detailed                   |
|:-----------------------------:|:---------------------------------------------------------:|
|     `details-get-seq.png`     |             GET of the "account details" page             |
| `details-newtransfer-seq.png` |             Submitting a "New transfer" form              |
|       `history-seq.png`       |                     History tracking                      |
|      `home-get-seq.png`       |                   GET of the home page                    |
|   `home-newAccount-seq.png`   |             Clicking the "New account" button             |
|        `login-seq.png`        |        GET of the "Login" page and login of a user        | 
|      `register-seq.png`       | GET of the "Register" page and registration of a new user |
|       `status-seq.png`        |       GET of the "Transfer accepted/rejected" pages       |

### Sequences for the RIA-only version (`ria-seq/`)

|             File              |                      Event/Action detailed                       |
|:-----------------------------:|:----------------------------------------------------------------:|
|  `authenticatedPost-seq.png`  |             Invocation of `Ajax::authenticatedPost`              |
| `details-newtransfer-seq.png` |             Interacting with the "New transfer" form             |
|    `details-show-seq.png`     |             Navigating to the "Account details" view             |
|      `home-get-seq.png`       |                       GET of the home page                       |
|   `home-newAccount-seq.png`   |                Clicking the "New account" button                 |
|        `login-seq.png`        |           GET of the "Login" page and login of a user            | 
|      `register-seq.png`       | Navigating to the "Register" view and registration of a new user |
|       `status-seq.png`        |     Interaction with the "Transfer accepted/rejected" modal      |

## Database diagrams

`er.png` contains the ER model of the database. Cardinalities are same-side, while keys have their name underlined.
`logic.png` is a schematization of the actual implementation of the database.