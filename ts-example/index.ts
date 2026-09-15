import  { spawnSync } from "node:child_process"
import { createInterface } from "node:readline/promises"

const rl = createInterface(
    process.stdin,
    process.stdout,
)

while (true) {
    const method = await rl.question("METHOD>$")
    if (method == "" || (method != "GET" && method != "POST")) continue

    const path = await rl.question("PATH>$")
    if (path == "") continue

    console.log(`sending : ${method} => ${path}`)

    const output = spawnSync(
        "java",
        [
            "-jar",
            "<jar>",
            "<libs dir>",
            method,
            path
        ]
    )

    const json = JSON.parse(output.stdout.toString())
    const response = await fetch(
        `https://ru.laneapi.com${path}`,
        {
            headers: {
                "x-accept-red": "0",
                "accept-language": "ru",
                "ldi": "<ldi>",
                "authorization": `Bearer <bearer token>`,
                "tz": "Europe/Moscow",
                "x-app-version": "207",
                "x-platform": "android",
                "x-theme": "light",
                "user-agent": "LaneMusic/1.0 (Android; Mobile)",
                "accept-encoding": "application/json",
                "x-core-token": json["x-core-token"],
                "x-client-meta": json["x-client-meta"],
                "x-request-trace-id": json["x-request-trace-id"],
            }
        }
    )

    console.log(`Response::status :: ${response.status}`)
    if (response.body != null)
        console.log(`Response::body :\n${await response.body.text()}`)
}