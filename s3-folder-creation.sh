#!/bin/bash

# Crear carpeta en el bucket de S3
aws s3api put-object --bucket bucketssumativa1 --key carpeta/

echo "Carpeta creada en el bucket S3: bucketssumativa1/carpeta/"
