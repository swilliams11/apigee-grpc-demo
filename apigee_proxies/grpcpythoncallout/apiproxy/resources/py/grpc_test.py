import base64

username = flow.getVariable("request.formparam.client_id")
password = flow.getVariable("request.formparam.client_secret")

base64string = base64.encodestring('%s:%s' % (username, password))[:-1]
authorization = "Basic "+base64string

flow.setVariable("authorizationParam",authorization)