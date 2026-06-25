package zeta.authz

import future.keywords.if
import future.keywords.in

# Regel 1: Definiert 'decision' für den FEHLERFALL.
decision := response if {
	failures := reasons
	count(failures) > 0
	response := {
		"allow": false,
		"reasons": failures,
	}
}

# Regel 2: Definiert 'decision' für den ERFOLGSFALL.
decision := response if {
	count(reasons) == 0
	response := {
		"allow": true,
		"ttl": {
			"access_token": 300,
			"refresh_token": 86400,
		},
	}
}

reasons[msg] if {
	not scopes_are_allowed
	msg := "One or more requested scopes are not allowed"
}

scopes_are_allowed if {
	allowed_scope_set := {"vsdservice"}
	requested_scope_set := {s | s := input.authorization_request.scopes[_]}
	count(requested_scope_set) > 0
	requested_scope_set - allowed_scope_set == set()
}
