package xyz.vexo.utils.api

/**
 * All endpoints of the Vexo API.
 *
 * Only the path lives here. The response format and the expected query
 * parameters of an endpoint are defined in its own typed wrapper (e.g. [DungeonsApi]).
 *
 * @property path The endpoint path relative to the API base URL
 */
enum class ApiEndpoint(val path: String) {
    DUNGEONS("dungeons")
}