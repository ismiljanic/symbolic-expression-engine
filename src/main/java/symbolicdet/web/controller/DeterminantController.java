package symbolicdet.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import symbolicdet.web.dto.DeterminantRequest;
import symbolicdet.web.dto.DeterminantResponse;
import symbolicdet.web.service.DeterminantWebService;

/**
 * Single REST controller.
 *
 * POST /api/determinant   — compute determinant (symbolic + numeric)
 * GET  /api/health        — liveness check
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DeterminantController {

    private final DeterminantWebService service;

    public DeterminantController(DeterminantWebService service) {
        this.service = service;
    }

    /**
     * Main computation endpoint.
     *
     * Example request body:
     * {
     *   "matrix": [["a","b"],["c","d"]],
     *   "variables": {"a": 1, "b": 2, "c": 3, "d": 4},
     *   "factorize": false,
     *   "detailedLU": false
     * }
     */
    @PostMapping("/determinant")
    public ResponseEntity<DeterminantResponse> compute(@RequestBody DeterminantRequest request) {

        if (request.isUseTridiagonal()) {
            Integer n = request.getTridiagonalSize();
            if (n == null || n < 2 || n > 10) {
                return ResponseEntity.badRequest().body(
                        DeterminantResponse.error("Tridiagonal size must be between 2 and 10."));
            }
        } else {
            if (request.getMatrix() == null || request.getMatrix().length == 0) {
                return ResponseEntity.badRequest().body(
                        DeterminantResponse.error("Matrix must not be empty."));
            }
            int n = request.getMatrix().length;
            if (n > 10) {
                return ResponseEntity.badRequest().body(
                        DeterminantResponse.error("Matrix size limited to 10×10. Use the CLI for larger matrices."));
            }
        }

        DeterminantResponse result = service.compute(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("ok");
    }
}