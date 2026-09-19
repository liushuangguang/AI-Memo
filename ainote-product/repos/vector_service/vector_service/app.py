from fastapi import FastAPI, APIRouter
import uvicorn

from vector_service.controller.vector_controller import vector_controller
from vector_service.controller.theme_controller import theme_controller
from vector_service.controller.work_flow_controller import work_flow_controller

app = FastAPI()
app.include_router(vector_controller,prefix='/vector')
app.include_router(theme_controller,prefix='/theme')
app.include_router(work_flow_controller,prefix='/workflow')

if __name__ == "__main__":
    uvicorn.run('__main__:app', host='0.0.0.0', port=6000, reload=True, workers=1)