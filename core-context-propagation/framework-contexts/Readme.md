| Context name             | transport propagation | dump/restore propagation |
|--------------------------|-----------------------|--------------------------|
| accept-language          |                 +     | +                        |
| allowed-headers          |                 +     | +                        |
| api version              |                 -     | +                        |
| request-id               |                 +     | +                        |
| x-version                |                 +     | +                        |
| x-version-name           |                 +     | +                        |
| x-nc-client-ip           |                 +     | +                        |
| request headers          |                 -     | -                        |
| business-process-id      |                 +     | +                        |
| originating-business-id  |                 +     | +                        |
|                          |                       |                          |
|                          |             security  |                          |
| authorization            |                 -     | +                        |
| customer-id              |                 +     | +                        |
| tenant                   |                 +     | +                        |
| who-am-I                 |                 -     | +                        |
| x-external-authorization |                 +     | +                        |
