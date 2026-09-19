import json
import logging
import os
import time
from typing import Optional, List

import numpy as np
import requests
from dotenv import load_dotenv
from pymilvus import Collection, connections, SearchResult, Hit

from vector_service.constants import EMBEDDING_URL, VECTOR_COLLECTION_NAME, THEME_COLLECTION_NAME
from vector_service.request.insert_request import NoteInformation
from vector_service.responses.note_insert_response import NoteInsertResult, RelatedItem

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class VectorService:
    def __init__(self):
        self.records = []
        load_dotenv()

    @staticmethod
    def get_embedding(content: str) -> List:
        api_key = os.getenv("SILICONFLOW_API_KEY", "").strip()
        if not api_key:
            raise RuntimeError("SILICONFLOW_API_KEY is not configured")

        payload = {
            "model": "BAAI/bge-large-zh-v1.5",
            "input": content,
            "encoding_format": "float"
        }
        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json"
        }
        try:
            response = requests.request("POST", EMBEDDING_URL, json=payload, headers=headers)
            response.raise_for_status()
            res = response.json()
        except requests.RequestException:
            logger.error("Embedding request failed")
            raise RuntimeError("Embedding request failed") from None
        return res['data'][0]['embedding']

    def insert_vector(self, user_id: str, notes: List[NoteInformation]):
        try:
            connections.connect("default", host="127.0.0.1", port="19530")
            intention_milvus = Collection(VECTOR_COLLECTION_NAME)
            logger.info(f"Number of entities in note collection: {intention_milvus.num_entities}")
            result = []
            for note in notes:
                note_emb = self.get_embedding(note.note_content)
                theme_emb = self.get_embedding(note.note_theme)
                search_query = f"user_id == '{user_id}' && note_id == '{note.note_id}'"
                if intention_milvus.query(search_query):
                    logger.info(f"Note with id {note.note_id} already exists for user {user_id}")
                    continue
                intention_milvus.insert(data=[[note_emb],[note.note_content],[note.note_id],[note.note_theme],[theme_emb],[user_id],[int(time.time())]])
                result.append(self.__find_related_notes(note.note_id, user_id, theme_emb))
            intention_milvus.flush()
            logger.info(f"Number of entities in collection: {intention_milvus.num_entities}")
            return result
        except Exception as e:
            logger.error(f"Error occurred while inserting vectors: {str(e)}")
            return []

    def __find_related_notes(self, note_id: str, user_id: str, content_to_compare_emb,
                             threshold: float = 0.7) -> NoteInsertResult:
        all_themes = self.__get_all_research_result(collection_name=THEME_COLLECTION_NAME,
                                                    content_to_compare_emb=content_to_compare_emb, user_id=user_id,
                                                    output_fields=None, anns_field='theme_embedding')
        filtered_search_result = self.__get_filtered_search_result(all_themes, threshold)
        related_items = []
        for f in filtered_search_result:
            related_items.append(RelatedItem(related_theme_id=f.entity.get('theme_id'),
                                             related_notes_ids=f.entity.get('related_note_ids').split(';')))
        return NoteInsertResult(note_id=note_id, related_item=related_items)

    def upsert_theme_vector(self, user_id: str, theme_id: str, theme_content: str):
        logger.info(f"Upserting theme vector for user {user_id} with theme id {theme_id}")
        connections.connect("default", host="127.0.0.1", port="19530")
        intention_milvus = Collection('note_theme')
        logger.info(f"Number of entities in theme collection: {intention_milvus.num_entities}")
        query = f"theme_id == '{theme_id}' && user_id == '{user_id}'"
        existing_theme = intention_milvus.query(query)
        if existing_theme:
            intention_milvus.delete(query)
        return self.__insert_new_theme(intention_milvus, theme_id, theme_content, user_id)

    def __insert_new_theme(self, intention_milvus, theme_id, theme_content, user_id):
        try:
            theme_emb = self.get_embedding(theme_content)
            all_notes = self.__get_all_research_result(collection_name=VECTOR_COLLECTION_NAME,
                                                       content_to_compare_emb=theme_emb, user_id=user_id,
                                                       output_fields=["note", "note_id", "time"], anns_field='theme_emb')
            if not all_notes:
                intention_milvus.insert(data=[[theme_emb],[theme_id],[theme_content],[user_id],[''],[int(time.time())]])
                return []
            filtered_search_result = self.__get_filtered_search_result(all_notes, 0.5)
            related_note_ids = [f.entity.get('note_id') for f in filtered_search_result]
            intention_milvus.insert(data=[[theme_emb],[theme_id],[theme_content],[user_id],[';'.join(related_note_ids)],[int(time.time())]])
            return related_note_ids
        except Exception as e:
            logger.error(f"Error occurred while upserting theme vector: {str(e)}")
            raise e

    def find_similar_notes(self, user_id: str, note: str, theme_id: str, threshold: float = 0.0) -> List:
        note_emb = self.get_embedding(note)
        return self.iterate_search_result(theme_id, user_id, note_emb, threshold)

    def iterate_search_result(self, theme_id, user_id=None, note=None, threshold=0.0) -> List:
        records_to_use = self.records if self.records else []
        all_search_result = self.__get_all_research_result(collection_name=VECTOR_COLLECTION_NAME,
                                                           content_to_compare_emb=note, user_id=user_id,
                                                           output_fields=["note", "time"], anns_field='note_emb')
        filtered_search_result = self.__get_filtered_search_result(all_search_result, threshold)
        if not filtered_search_result:
            return []
        potential_records = [f for f in filtered_search_result if f.entity.get('note_id') not in records_to_use and f.entity.get('theme_id') == theme_id]
        if not potential_records:
            return []
        for item in potential_records:
            content_of_item = item.entity.get('note')
            if content_of_item == note:
                continue
            found_note_id = item.entity.get('note_id')
            self.records.append(found_note_id) if found_note_id not in self.records else None
            self.iterate_search_result(theme_id, user_id, content_of_item, threshold)
        return self.records

    def __get_all_research_result(self, collection_name: str, content_to_compare_emb, user_id,
                                  output_fields: Optional[List[str]], anns_field: str) -> SearchResult:
        connections.connect("default", host="127.0.0.1", port="19530")
        note_milvus = Collection(collection_name)
        query = f"user_id == '{user_id}'"
        search_params = {"metric_type": "COSINE", "params": {"nprobe": 10}}
        query_vectors = np.array([content_to_compare_emb]).astype('float32')
        return note_milvus.search(data=query_vectors, anns_field=anns_field, param=search_params, limit=10,
                                  output_fields=output_fields, expr=query)

    @staticmethod
    def __get_filtered_search_result(search_result: SearchResult, threshold: float = 0.0) -> List[Hit]:
        return [r for r in search_result[0] if r.distance >= threshold]

    def run_work_flow(self, user_id: str, theme_name: str, theme_content: str, workflow_id: str):
        url = 'https://api.coze.cn/v1/workflow/run'
        headers = {"Authorization": f"Bearer {os.getenv('WORKFLOW_RUN_TOKEN')}", "Content-Type": "application/json"}
        data = {"parameters": {"user_id": user_id, "theme_name": theme_name, "theme_content": theme_content}, "workflow_id": workflow_id}
        return self.__get_retrival_with_retry(url, headers, data, 3)

    def __get_retrival_with_retry(self, url: str, headers: dict, data: dict, retry: int):
        count = 0
        retrival = self.__run_workflow(url, headers, data)
        while not retrival and retry > count:
            time.sleep(1)
            retrival = self.__run_workflow(url, headers, data)
            count += 1
        return retrival

    @staticmethod
    def __run_workflow(url: str, headers: dict, data: dict):
        response = requests.request("POST", url=url, headers=headers, json=data)
        response_json = response.json()
        try:
            out_put = json.loads(response_json.get('data')).get('output')
        except Exception:
            out_put = []
        return out_put
