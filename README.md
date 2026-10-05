# Phoebus service chart

Chart to run phoebus services:

- *save-and-restore* 
- *scan-server* 

- alarm-config-logger   *needs kafka* **not yet supported**
- alarm-logger *needs kafka* **not yet supported**
- alarm-server *needs kafka* **not yet supported**
- archive-engine **not yet supported**


## Olog → ARGUS Knowledge Hub

With `phoebusservice: olog` and `argusUpload.enabled: true`, the CronJob `<release>-argus-upload` sends
the Olog entries of the last `argusUpload.lookbackDays` (default 2) to ARGUS Knowledge Hub every night,
where each becomes a Logbook Entry document of the facility's workspace. Re-sending is safe: unchanged
entries are left alone and edited ones become new revisions.

| Value | Meaning |
|---|---|
| `argusUpload.url` | the ARGUS API, e.g. `https://argus-hub-api.example.org` (required) |
| `argusUpload.facility` | the logbook's name in ARGUS (default: the beamline/namespace) |
| `argusUpload.entryUrl` | where a person opens an entry, with `{id}` |
| `argusUpload.tokenSecret` | the Secret holding the robot token (default `argus-olog-upload`, key `token`) |
| `argusUpload.all` | `true` for one run to send the whole history |
| `argusUpload.schedule` | default `30 1 * * *` |

The token is an ARGUS robot token of the facility's workspace (*Daily logbook upload* preset), created by
hand, never committed:

```bash
kubectl -n <beamline> create secret generic argus-olog-upload --from-literal=token='argus_bot_…'
```

The job honours `https_proxy`/`http_proxy` and reaches the facility's Olog directly. The script,
`files/olog_to_argus.py`, is a copy of `tools/olog-to-argus/olog_to_argus.py` in the ARGUS repository.
