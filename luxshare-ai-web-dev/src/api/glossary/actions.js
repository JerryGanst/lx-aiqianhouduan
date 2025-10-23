import request from '@/utils/request.js'

const API = Object.freeze({
  QUERY_GLOSSARY: '/glossary',
  UPDATE_GLOSSARY: '/glossary/glossary_update'
})

export const queryGlossaryItems = payload => request.post(API.QUERY_GLOSSARY, payload)

export const updateGlossaryItems = payload => request.post(API.UPDATE_GLOSSARY, payload)
