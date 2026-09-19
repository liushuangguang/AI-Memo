import importlib
import importlib.util
import sys
import types
import unittest
from unittest.mock import Mock, patch


def load_service_module():
    package = types.ModuleType("vector_service")
    package.__path__ = []
    constants = types.ModuleType("vector_service.constants")
    constants.EMBEDDING_URL = "https://example.invalid/embeddings"
    constants.VECTOR_COLLECTION_NAME = "notes"
    constants.THEME_COLLECTION_NAME = "note_theme"
    request_module = types.ModuleType("vector_service.request.insert_request")
    request_module.NoteInformation = object
    response_module = types.ModuleType("vector_service.responses.note_insert_response")
    response_module.NoteInsertResult = object
    response_module.RelatedItem = object
    pymilvus = types.ModuleType("pymilvus")
    pymilvus.Collection = object
    pymilvus.connections = Mock()
    pymilvus.SearchResult = object
    pymilvus.Hit = object
    modules = {
        "vector_service": package,
        "vector_service.constants": constants,
        "vector_service.request.insert_request": request_module,
        "vector_service.responses.note_insert_response": response_module,
        "pymilvus": pymilvus,
    }
    with patch.dict(sys.modules, modules):
        spec = importlib.util.spec_from_file_location(
            "vector_service.services.vector_service", "vector_service/services/vector_service.py"
        )
        module = importlib.util.module_from_spec(spec)
        sys.modules[spec.name] = module
        spec.loader.exec_module(module)
        return module


class EmbeddingAuthTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.service_module = load_service_module()

    def test_missing_key_does_not_send_request(self):
        with patch.dict("os.environ", {}, clear=True), patch.object(self.service_module.requests, "request") as request:
            with self.assertRaisesRegex(RuntimeError, "SILICONFLOW_API_KEY"):
                self.service_module.VectorService.get_embedding("text")
        request.assert_not_called()

    def test_key_is_trimmed_and_used_as_bearer_token(self):
        response = Mock()
        response.json.return_value = {"data": [{"embedding": [1.0]}]}
        with patch.dict("os.environ", {"SILICONFLOW_API_KEY": "  test-secret  "}), patch.object(
                self.service_module.requests, "request", return_value=response) as request:
            result = self.service_module.VectorService.get_embedding("text")
        self.assertEqual(result, [1.0])
        self.assertEqual(request.call_args.kwargs["headers"]["Authorization"], "Bearer test-secret")

    def test_http_error_does_not_include_key(self):
        response = Mock()
        response.raise_for_status.side_effect = self.service_module.requests.HTTPError("upstream failure")
        with patch.dict("os.environ", {"SILICONFLOW_API_KEY": "test-secret"}), patch.object(
                self.service_module.requests, "request", return_value=response):
            with self.assertRaisesRegex(RuntimeError, "Embedding request failed") as error:
                self.service_module.VectorService.get_embedding("text")
        self.assertNotIn("test-secret", str(error.exception))

    def test_timeout_does_not_include_key(self):
        with patch.dict("os.environ", {"SILICONFLOW_API_KEY": "test-secret"}), patch.object(
                self.service_module.requests, "request", side_effect=self.service_module.requests.Timeout("timed out")):
            with self.assertRaisesRegex(RuntimeError, "Embedding request failed") as error:
                self.service_module.VectorService.get_embedding("text")
        self.assertNotIn("test-secret", str(error.exception))


if __name__ == "__main__":
    unittest.main()
