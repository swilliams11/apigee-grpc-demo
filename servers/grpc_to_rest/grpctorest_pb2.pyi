from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Optional as _Optional

DESCRIPTOR: _descriptor.FileDescriptor

class GrpcToRestRequest(_message.Message):
    __slots__ = ("fname", "lname", "city")
    FNAME_FIELD_NUMBER: _ClassVar[int]
    LNAME_FIELD_NUMBER: _ClassVar[int]
    CITY_FIELD_NUMBER: _ClassVar[int]
    fname: str
    lname: str
    city: str
    def __init__(self, fname: _Optional[str] = ..., lname: _Optional[str] = ..., city: _Optional[str] = ...) -> None: ...

class GrpcToRestReply(_message.Message):
    __slots__ = ("message",)
    MESSAGE_FIELD_NUMBER: _ClassVar[int]
    message: str
    def __init__(self, message: _Optional[str] = ...) -> None: ...
